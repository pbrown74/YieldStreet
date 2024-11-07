package com.yieldstreet.service;

import com.yieldstreet.dto.*;
import com.yieldstreet.exception.*;
import com.yieldstreet.entity.*;
import com.yieldstreet.rabbit.RabbitMQSender;
import com.yieldstreet.repository.AccreditationHistoryRepository;
import com.yieldstreet.repository.AccreditationRepository;
import com.yieldstreet.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * this class provides methods for working with accreditation data via REST endpoints.
 * in this class we work with DTO classes, the repository layer deals with Entity versions of these DTOs,
 * so some mapping happens to and from formats.
 */
@Service
public class AccreditationService {
    private static Logger logger = LoggerFactory.getLogger(AccreditationService.class);

    @Autowired
    private AccreditationRepository accreditationRepository;

    @Autowired
    private AccreditationHistoryRepository accreditationHistoryRepository;

    @Autowired
    private DocumentRepository docRepository;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    /**
     * insertion to MySQL of an accreditation, this is seperated into a Document and Accreditation
     * hence the need for a transaction around the two inserts. if there is a pending accreditation
     * for the inserting user, then we disallow the insertion.
     * @param accreditationDTO
     * @return
     */
    @Transactional
    public AccreditationIdDTO insertAccreditation(AccreditationDTO accreditationDTO) {
        validateUserId(accreditationDTO);
        validateMimeType(accreditationDTO);
        // this is safe to check here because the initial saving of the accreditation is done synchronously,
        // so there is not a creation hidden in the queue. only updates are done via the queue.
        List<Accreditation> existing = accreditationRepository.findByUserIdIs(accreditationDTO.getUserId());
        Optional<Accreditation> pendingAccreditationForUser = findPendingAccreditation(existing);
        if(pendingAccreditationForUser.isPresent()){
            String pendingAccId = pendingAccreditationForUser.get().getAccreditationId();
            logger.error("Found pending accreditation: " + pendingAccId);
            throw new InvalidInitialStateException(pendingAccId);
        }
        // Spring managed DB transaction around these two inserts (see @Transactional)
        Document doc = docRepository.save(buildDocument(accreditationDTO));
        Accreditation acc = accreditationRepository.save(buildAccreditation(accreditationDTO, doc));
        // save went ok, build a DTO for caller
        logger.debug("Saved new accreditation: " + acc.getAccreditationId() + " for user: " + acc.getUserId());
        AccreditationIdDTO accreditationIdDTO = new AccreditationIdDTO();
        accreditationIdDTO.setAccreditationId(acc.getAccreditationId());
        return accreditationIdDTO;
    }

    /**
     * update the state of an accreditation, this can be done by users and also by the scheduled expiry job.
     * in this method we do not directly update the state in the database. we send an event to RabbitMQ to do
     * both the update and also to manage the status audit. since the scheduled job and this endpoint both go
     * via the queue we have reduced ordering issues by single threading the updates onto the Quartz thread.
     * @param accreditationId
     * @param outcomeDTO
     * @return
     */
    public AccreditationIdDTO updateAccreditation(String accreditationId,
                                                  AccreditationOutcomeDTO outcomeDTO) {
        validateUUID(accreditationId);
        validateOutcome(outcomeDTO);
        Optional<Accreditation> saved = accreditationRepository.findById(accreditationId);
        if(saved.isPresent()){
            Accreditation current = saved.get();
            AccreditationStatus targetStatus = AccreditationStatus.from(outcomeDTO.getOutcome());
            // FAILED is a terminal state, so this is a safe check to do in this endpoint. if a FAILED message is in the queue then we retest in receiver.
            // two users could try and CONFIRM a PENDING acc at the same time, so this cannot be checked here, it must be done via queue.
            if(current.getStatus()==AccreditationStatus.FAILED){
                // already FAILED so any state change is disallowed.
                // if there is a FAILED update on the queue then this will be trapped later in the queue receiver.
                logger.error("Accreditation already failed, cannot change state further: " + current.getAccreditationId());
                throw new InvalidStateChangeException(accreditationId, current.getStatus(), targetStatus);
            }
            current.setStatus(targetStatus);
            try{
                // if the message is queued, its considered done although there can be a race condition with GET (see readme)
                rabbitMQSender.send(current);
                logger.debug("Accreditation state change event sent to RabbitMQ for accreditation: " + current.getAccreditationId());
            }
            catch(Exception e){
                throw new AccreditationNotUpdatedException(accreditationId);
            }
            AccreditationIdDTO dto = new AccreditationIdDTO();
            dto.setAccreditationId(accreditationId);
            return dto;
        }
        else{
            throw new AccreditationNotFoundException(accreditationId);
        }
    }

    /**
     * there can be a race condition between this GET and an accreditation status change which is on the queue but not yet processed.
     * in the current implementation there is nothing to deal with that. we could cache the updated accreditation in service before
     * sending the message to RabbitMQ, if we had a transactional cache this would be safe. Then the GET method would have the latest
     * version of the accreditation and RabbitMQ would update the persisted state sometime later. This would be non-trivial to implement.
     * If the data is not critical then some systems live with the race condition and perhaps use a version number to optimistically lock
     * so updates on a stale version are not possible. This is life in a real time system.
     * @param userId
     * @return
     */
    public UserAccreditationsDTO getUserAccreditations(String userId) {
        Map<String, AccreditationTypeAndStatusDTO> dtos = new HashMap<>();
        List<Accreditation> accs = accreditationRepository.findByUserIdIs(userId);
        for(Accreditation acc : accs){
            dtos.put(acc.getAccreditationId(), new AccreditationTypeAndStatusDTO(acc.getStatus().name(), acc.getType().name()));
        }
        UserAccreditationsDTO userAccreditationsDTO = new UserAccreditationsDTO(userId, dtos);
        return userAccreditationsDTO;
    }

    /**
     * not a required end point but i added it in because it was useful in testing
     * @param accreditationId
     * @return
     */
    public AccreditationHistoricTypeAndStatusDTO[] getAccreditationHistories(String accreditationId) {
        Optional<Accreditation> current = accreditationRepository.findById(accreditationId);
        List<AccreditationHistoricTypeAndStatusDTO> dtos = new Vector<>();
        if(current.isPresent()){
            Accreditation acc = current.get();
            List<AccreditationHistory> accs = accreditationHistoryRepository.findByAccreditationIdIs(accreditationId);
            for(AccreditationHistory accHistory : accs){
                Accreditation temp = new Accreditation();
                temp.setStatus(accHistory.getOldStatus());
                temp.setAccreditationId(acc.getAccreditationId());
                temp.setType(acc.getType());
                temp.setLastUpdateTime(accHistory.getLastUpdateTime());
                dtos.add(new AccreditationHistoricTypeAndStatusDTO(temp.getStatus().name(), temp.getType().name(), temp.getLastUpdateTime()));
            }
        }
        return dtos.toArray(new AccreditationHistoricTypeAndStatusDTO[]{});
    }

    /**
     * helpers follow
     */

    private Document buildDocument(AccreditationDTO accreditationDTO){
        Document doc = new Document();
        doc.setContent(accreditationDTO.getDocument().getContent());
        doc.setMimeType(accreditationDTO.getDocument().getMimeType());
        doc.setName(accreditationDTO.getDocument().getName());
        return doc;
    }

    private Accreditation buildAccreditation(AccreditationDTO accreditationDTO, Document doc){
        Accreditation acc = new Accreditation();
        acc.setUserId(accreditationDTO.getUserId());
        acc.setType(accreditationDTO.getType());
        acc.setDocument(doc);
        acc.setStatus(AccreditationStatus.PENDING);
        return acc;
    }

    private Optional<Accreditation> findPendingAccreditation(List<Accreditation> accreditations){
        if(!accreditations.isEmpty()){
            for(Accreditation acc: accreditations){
                if(acc.getStatus()==AccreditationStatus.PENDING){
                    return Optional.of(acc);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * validation methods follow
     */

    private void validateUUID(String id){
        try{
            UUID.fromString(id);
        }
        catch(Exception e){
            throw new InvalidIdFormatException(id);
        }
    }

    private void validateOutcome(AccreditationOutcomeDTO outcomeDTO){
        try{
            AccreditationStatus.from(outcomeDTO.getOutcome());
        }
        catch(IllegalArgumentException e){
            throw new InvalidOutcomeException(outcomeDTO);
        }
    }

    private void validateUserId(AccreditationDTO accreditation){
        // TODO we should have a User entity in the database to check against
    }

    private void validateMimeType(AccreditationDTO accreditation){
        try{
            MimeType.valueOf(accreditation.getDocument().getMimeType());
        }
        catch(Exception e){
            throw new InvalidMimeTypeException(accreditation.getDocument().getMimeType());
        }
    }

}