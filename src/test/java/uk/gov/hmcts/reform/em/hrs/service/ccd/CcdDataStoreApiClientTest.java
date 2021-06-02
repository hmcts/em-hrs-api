package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CcdDataStoreApiClientTest {

    private static final String JURISDICTION = "HRS";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final Long CASE_ID = 123456789L;
    private static final String CREATE_CASE = "createCase";
    private static final String ADD_RECORDING_FILE = "manageFiles";
    private static final String USER_TOKEN = "userToken";
    private static final String SERVICE_TOKEN = "serviceToken";
    private static final String USER_ID = "123456";
    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final HearingRecordingDto HEARING_RECORDING_DTO = HearingRecordingDto.builder()
        .recordingRef("recordingRef").build();

    @Mock
    SecurityService securityService;

    @Mock
    CaseDataContentCreator caseDataContentCreator;

    @Mock
    CoreCaseDataApi coreCaseDataApi;

    @InjectMocks
    CcdDataStoreApiClient underTest;

    @Test
    void shouldCreateCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN)).when(securityService).getTokens();
        StartEventResponse startEventResponse = StartEventResponse.builder().build();
        doReturn(startEventResponse).when(coreCaseDataApi)
            .startCase(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE, CREATE_CASE);
        CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).build();
        JsonNode data = JsonNodeFactory.instance.objectNode();
        doReturn(data).when(caseDataContentCreator).createCaseStartData(HEARING_RECORDING_DTO, RECORDING_ID);
        CaseDataContent caseData = CaseDataContent.builder().data(data)
            .event(Event.builder().build()).build();
        doReturn(caseDetails).when(coreCaseDataApi).submitForCaseworker(
            USER_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION, CASE_TYPE, false,caseData
        );

        Long caseId = underTest.createCase(RECORDING_ID, HEARING_RECORDING_DTO);

        assertEquals(CASE_ID, caseId);
    }

    @Test
    void shouldUpdateCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN)).when(securityService).getTokens();
        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder().id(CASE_ID).build())
            .build();
        doReturn(startEventResponse).when(coreCaseDataApi).startEvent(
            USER_TOKEN, SERVICE_TOKEN, String.valueOf(CASE_ID), ADD_RECORDING_FILE
        );
        CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).build();
        JsonNode data = JsonNodeFactory.instance.objectNode();
        doReturn(data).when(caseDataContentCreator)
            .createCaseUpdateData(startEventResponse.getCaseDetails().getData(), RECORDING_ID, HEARING_RECORDING_DTO);
        CaseDataContent caseData = CaseDataContent.builder().data(data)
            .event(Event.builder().build()).build();
        doReturn(caseDetails).when(coreCaseDataApi).submitEventForCaseWorker(
            USER_TOKEN, SERVICE_TOKEN, USER_ID,
            JURISDICTION, CASE_TYPE, String.valueOf(CASE_ID), false, caseData
        );

        Long caseId = underTest.updateCaseData(CASE_ID, RECORDING_ID, HEARING_RECORDING_DTO);

        assertEquals(123456789L, caseId);
    }
}
