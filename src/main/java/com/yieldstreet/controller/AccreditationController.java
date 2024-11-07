package com.yieldstreet.controller;

import com.yieldstreet.dto.*;
import com.yieldstreet.service.AccreditationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * main entry point into the backend
 *
 * three endpoints:
 *   admin facing POST
 *   admin facing PUT
 *   client facing GET
 *
 * see the service layer for the business logic
 */
@RestController
@RequestMapping(path="/user")
public class AccreditationController {
    @Autowired
    private AccreditationService accreditationService;

    /**
     * endpoint for admin use
     * @param accreditation
     * @return
     */
    @PostMapping(path= "/accreditation")
    public @ResponseBody AccreditationIdDTO insertAccreditation(
            @RequestBody AccreditationDTO accreditation) {
        return accreditationService.insertAccreditation(accreditation);
    }

    /**
     * endpoint for admin use
     * @param accreditationId
     * @param outcomeDTO
     * @return
     */
    @PutMapping(path= "/accreditation/{accreditation_id}")
    public @ResponseBody AccreditationIdDTO updateAccreditation(
            @PathVariable("accreditation_id") String accreditationId,
            @RequestBody AccreditationOutcomeDTO outcomeDTO) {
        return accreditationService.updateAccreditation(accreditationId, outcomeDTO);
    }

    /**
     * endpoint for client facing traffic
     * @param userId
     * @return
     */
    @GetMapping(path= "/{user_id}/accreditation")
    public @ResponseBody UserAccreditationsDTO getUserAccreditations(
            @PathVariable("user_id") String userId) {
        return accreditationService.getUserAccreditations(userId);
    }

    /**
     * additional endpoint useful for testing
     * @param accreditationId
     * @return
     */
    @GetMapping(path= "/history/{accreditation_id}")
    public @ResponseBody
    AccreditationHistoricTypeAndStatusDTO[] getAccreditationHistories(
            @PathVariable("accreditation_id") String accreditationId) {
        return accreditationService.getAccreditationHistories(accreditationId);
    }

}