package com.yieldstreet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.yieldstreet.dto.*;
import com.yieldstreet.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The approach here is to test via the endpoints using the Spring RestTemplate.
 * No mocking is used, all the real code and backend is being tested. This is an integration test.
 * The idea of this kind of test is to make it as close to PROD as possible.
 * I would separately have unit tests around code in complex classes.
 * We build DTOs and use the Spring infrastructure to create JSON and call the endpoints over HTTP.
 * We test assertions using DTO responses.
 * The test harness should be run is isolation without needing to start the backend separately,
 * this is handled for us by TestContainers.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = AssignmentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AssignmentApplicationTests {
	private RestTemplate restTemplate;
	private HttpHeaders headers;
	private ObjectMapper objectMapper;
	private final static String GETAccreditationUrl = "http://localhost:${server.port}/user/{userId}/accreditation";
	private final static String GETAccreditationHistoryUrl = "http://localhost:${server.port}/user/history/{accreditationId}";
	private final static String POSTAccreditationUrl = "http://localhost:${server.port}/user/accreditation";
	private final static String PUTAccreditationUrl = "http://localhost:${server.port}/user/accreditation/{accreditationId}";
	private final static String USER_ID_NAME = "userId";
	private final static String USER_ID_VALUE = "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V";
	private final static String ACC_ID_NAME = "accreditationId";
	private final static String DOC_NAME = "2018.pdf";
	private final static String MIME_TYPE = "application/pdf";
	private final static String CONTENT = "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==";
	private final static Map<String, String> USER_ID_PAIR = Collections.singletonMap(USER_ID_NAME, USER_ID_VALUE);
	@Value("${server.port}")
	private String serverPort;

	public AssignmentApplicationTests() {
		restTemplate = new RestTemplate();
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new ParameterNamesModule());
	}

	/**
	 * this test will cover most of the functionality:
	 *    successful GET/PUT/POST endpoints
	 *    failing PUT (bad argument)
	 *    failing POST due to pending accreditation
	 *    successful scheduling of the CONFIRM->EXPIRED transition which runs 1 minute after CONFIRMED is hit (configured in context)
	 *    check the status history is correct for the accreditation PENDING->CONFIRMED->EXPIRED
	 *
	 *  more tests could be added along similar lines, one could check race condition between the asynchronous expiry and GET
	 */
	@Test
	public void insert_Pending_Accreditation_Then_Confirm_And_Check_It_Expires() {
		// POST good accreditation
		AccreditationDTO accreditationDTO = buildAccreditation();
		AccreditationIdDTO accreditationIdDTO = doPOST(accreditationDTO, AccreditationIdDTO.class);
		String accreditationId = accreditationIdDTO.getAccreditationId();
		assertThat(!accreditationId.isEmpty());
		assertThat(isUUID(accreditationId));

		// POST second one for same userId, this should fail because of pending accreditation on userId
		AccreditationDTO secondAccreditationDTO = copy(accreditationDTO);
		HttpStatusCode sc = doPOSTVerbose(secondAccreditationDTO);
		assertThat(sc==HttpStatus.METHOD_NOT_ALLOWED);

		// GET the accreditation, so we verify it was created
		UserAccreditationsDTO userAccreditationsDTO = doGET(UserAccreditationsDTO.class, USER_ID_PAIR);
		assertThat(userAccreditationsDTO.getAccreditationStatuses().size()==1);
		AccreditationTypeAndStatusDTO status = userAccreditationsDTO.getAccreditationStatuses().get(accreditationId);
		assertThat(AccreditationStatus.from(status.getAccreditationStatus())==AccreditationStatus.PENDING);
		assertThat(AccreditationType.valueOf(status.getAccreditationType())==AccreditationType.BY_INCOME);

		// PUT a bad accreditation to check the HTTP status code
		AccreditationOutcomeDTO outcomeDTO = new AccreditationOutcomeDTO(AccreditationStatus.CONFIRMED.name());
		sc = doPUT(outcomeDTO, Collections.singletonMap(ACC_ID_NAME, "bad accreditation id"));
		assertThat(sc==HttpStatus.BAD_REQUEST);

		// PUT a good accreditation to modify the state, asynchronous processing so do a little delay at the end
		outcomeDTO = new AccreditationOutcomeDTO(AccreditationStatus.CONFIRMED.name());
		sc = doPUT(outcomeDTO, Collections.singletonMap(ACC_ID_NAME, accreditationIdDTO.getAccreditationId()));
		assertThat(sc==HttpStatus.OK);

		// wait for asynch procesing of update message to complete
		sleep(5000);

		// GET the first accreditation to verify it was changed to CONFIRMED by doPUT, in a minute check it was pushed to EXPIRED
		userAccreditationsDTO = doGET(UserAccreditationsDTO.class, USER_ID_PAIR);
		status = userAccreditationsDTO.getAccreditationStatuses().get(accreditationId);
		assertThat(userAccreditationsDTO.getAccreditationStatuses().size()==1);
		assertThat(AccreditationStatus.from(status.getAccreditationStatus())==AccreditationStatus.CONFIRMED);
		assertThat(AccreditationType.valueOf(status.getAccreditationType())==AccreditationType.BY_INCOME);

		// POST second one again, this should work now because of first pending accreditation having move into CONFIRMED
		sc = doPOSTVerbose(secondAccreditationDTO);
		assertThat(sc==HttpStatus.OK);

		// GET the users accreditations just to check we have 2 now
		userAccreditationsDTO = doGET(UserAccreditationsDTO.class, USER_ID_PAIR);
		assertThat(userAccreditationsDTO.getAccreditationStatuses().size()==2);

		// wait for asynch processing of scheduled update message to complete in the Quartz scheduler, for the CONFIRMED accreditation
		sleep(90000);

		// GET the first accreditation to check it was pushed to EXPIRED, the 1 minute delay is configured in the test context
		userAccreditationsDTO = doGET(UserAccreditationsDTO.class, USER_ID_PAIR);
		status = userAccreditationsDTO.getAccreditationStatuses().get(accreditationId);
		assertThat(AccreditationStatus.from(status.getAccreditationStatus())==AccreditationStatus.EXPIRED);
		assertThat(AccreditationType.valueOf(status.getAccreditationType())==AccreditationType.BY_INCOME);

		// GET the history of the first accreditation, it should be PENDING->CONFIRMED (EXPIRED is not in the history since its current)
		AccreditationHistoricTypeAndStatusDTO[] historyDTOs = doGET(GETAccreditationHistoryUrl,
				AccreditationHistoricTypeAndStatusDTO[].class, Collections.singletonMap(ACC_ID_NAME, accreditationId));
		assertThat(historyDTOs.length==2);
		Set<AccreditationStatus> statuses = toStatuses(historyDTOs);
		assertThat(statuses.contains(AccreditationStatus.PENDING));
		assertThat(statuses.contains(AccreditationStatus.CONFIRMED));
	}

	/**
	 * helpers below
	 */

	private Set<AccreditationStatus> toStatuses(AccreditationHistoricTypeAndStatusDTO[] historyDTOs){
		Set<AccreditationStatus> ss = new HashSet<>();
		for(AccreditationHistoricTypeAndStatusDTO dto : historyDTOs){
			ss.add(AccreditationStatus.from(dto.getAccreditationStatus()));
		}
		return ss;
	}

	private boolean isUUID(String s){
		try{
			UUID.fromString(s);
			return true;
		}
		catch(Exception ignore){
			return false;
		}
	}

	private void sleep(long millis){
		try{
			Thread.sleep(millis);
		}
		catch(Exception ignore){
		}
	}

	private AccreditationDTO buildAccreditation(){
		DocumentDTO document = new DocumentDTO();
		document.setName(DOC_NAME);
		document.setMimeType(MIME_TYPE);
		document.setContent(CONTENT);
		AccreditationDTO accreditationDTO = new AccreditationDTO();
		accreditationDTO.setUserId(USER_ID_VALUE);
		accreditationDTO.setType(AccreditationType.BY_INCOME);
		accreditationDTO.setDocument(document);
		return accreditationDTO;
	}

	private <Input,Output> Output doPOST(Input bodyDTO, Class<Output> clazz){
		Output output = restTemplate.postForObject(
				expandPort(POSTAccreditationUrl),
				bodyDTO,
				clazz);
		return output;
	}

	private <Input> HttpStatusCode doPOSTVerbose(Input bodyDTO){
		HttpEntity<Input> entity = new HttpEntity<>(bodyDTO, headers);
		ResponseEntity<String> out;
		try {
			out = restTemplate.exchange(expandPort(POSTAccreditationUrl),
					HttpMethod.POST, entity, String.class);
		} catch(HttpStatusCodeException e) {
			return e.getStatusCode();
		}
		return out.getStatusCode();
	}

	private <Output> Output doGET(Class<Output> clazz, Map<String, String> urlParams){
		return doGET(GETAccreditationUrl, clazz, urlParams);
	}

	private <Output> Output doGET(String url, Class<Output> clazz, Map<String, String> urlParams){
		Output output = restTemplate.getForObject(
				expandPort(url),
				clazz,
				urlParams);
		return output;
	}

	private <Input> HttpStatusCode doPUT(Input bodyDTO, Map<String, String> urlParams){
		HttpEntity<Input> entity = new HttpEntity<>(bodyDTO, headers);
		ResponseEntity<String> out;
		try {
			out = restTemplate.exchange(expandPort(PUTAccreditationUrl),
					HttpMethod.PUT, entity, String.class, urlParams);
		} catch(HttpStatusCodeException e) {
			return e.getStatusCode();
		}
		return out.getStatusCode();
	}

	private String expandPort(String url){
		return url.replace("${server.port}", this.serverPort);
	}

	private AccreditationDTO copy(AccreditationDTO dto){
		try{
			return (AccreditationDTO)dto.clone();
		}
		catch(Exception e){
			throw new IllegalStateException(e);
		}
	}

}
