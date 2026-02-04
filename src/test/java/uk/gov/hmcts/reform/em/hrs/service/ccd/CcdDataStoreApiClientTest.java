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
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.model.TtlCcdObject;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdDataStoreApiClientTest {

    private static final String JURISDICTION = "HRS";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final Long CASE_ID = 123456789L;
    private static final String CREATE_CASE = "createCase";
    private static final String AMEND_CASE = "editCaseDetails";
    private static final String ADD_RECORDING_FILE = "manageFiles";
    private static final String USER_TOKEN = "userToken";
    private static final String SERVICE_TOKEN = "serviceToken";
    private static final String USER_ID = "123456";
    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final LocalDate ttl = LocalDate.now();
    private static final HearingRecordingDto HEARING_RECORDING_DTO = HearingRecordingDto.builder()
        .recordingRef("recordingRef").build();

    @Mock
    SecurityService securityService;

    @Mock
    CaseDataContentCreator caseDataContentCreator;

    @Mock
    CoreCaseDataApi coreCaseDataApi;

    @Mock
    TtlService ttlService;

    @InjectMocks
    CcdDataStoreApiClient underTest;

    @Test
    void shouldCreateCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        StartEventResponse startEventResponse = StartEventResponse.builder().build();

        doReturn(startEventResponse).when(coreCaseDataApi)
            .startCase(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE, CREATE_CASE);

        CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).build();

        JsonNode data = JsonNodeFactory.instance.objectNode();

        doReturn(data).when(caseDataContentCreator).createCaseStartData(HEARING_RECORDING_DTO, RECORDING_ID, ttl);

        CaseDataContent caseData = CaseDataContent.builder().data(data)
            .event(Event.builder().build()).build();

        doReturn(caseDetails).when(coreCaseDataApi).submitForCaseworker(
            USER_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION, CASE_TYPE, false, caseData
        );

        Long caseId = underTest.createCase(RECORDING_ID, HEARING_RECORDING_DTO, ttl);

        assertEquals(CASE_ID, caseId);
    }

    @Test
    void shouldThrowCcdUploadExceptionDuringCreateCaseGetTokens() {
        doThrow(CcdUploadException.class).when(securityService).createTokens();

        assertThatExceptionOfType(CcdUploadException.class).isThrownBy(() -> underTest.createCase(
            RECORDING_ID,
            HEARING_RECORDING_DTO,
            ttl
        ));
    }

    @Test
    void shouldThrowCcdUploadExceptionDuringStartCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        doThrow(CcdUploadException.class).when(coreCaseDataApi)
            .startCase(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE, CREATE_CASE);

        assertThatExceptionOfType(CcdUploadException.class).isThrownBy(() -> underTest.createCase(
            RECORDING_ID,
            HEARING_RECORDING_DTO,
            ttl
        ));
    }


    @Test
    void shouldThrowCcdUploadExceptionDuringCreateCaseCreateData() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        StartEventResponse startEventResponse = StartEventResponse.builder().build();

        doReturn(startEventResponse).when(coreCaseDataApi)
            .startCase(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE, CREATE_CASE);

        doThrow(CcdUploadException.class).when(caseDataContentCreator)
            .createCaseStartData(HEARING_RECORDING_DTO, RECORDING_ID, ttl);

        assertThatExceptionOfType(CcdUploadException.class).isThrownBy(() -> underTest.createCase(
            RECORDING_ID,
            HEARING_RECORDING_DTO,
            ttl
        ));
    }


    @Test
    void shouldThrowCcdUploadExceptionDuringCreateCaseSubmitForCaseworker() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        StartEventResponse startEventResponse = StartEventResponse.builder().build();

        doReturn(startEventResponse).when(coreCaseDataApi)
            .startCase(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE, CREATE_CASE);

        JsonNode data = JsonNodeFactory.instance.objectNode();

        doReturn(data).when(caseDataContentCreator).createCaseStartData(HEARING_RECORDING_DTO, RECORDING_ID, ttl);

        CaseDataContent caseData = CaseDataContent.builder().data(data)
            .event(Event.builder().build()).build();

        doThrow(CcdUploadException.class).when(coreCaseDataApi).submitForCaseworker(
            USER_TOKEN, SERVICE_TOKEN, USER_ID, JURISDICTION, CASE_TYPE, false, caseData
        );


        assertThatExceptionOfType(CcdUploadException.class).isThrownBy(() -> underTest.createCase(
            RECORDING_ID,
            HEARING_RECORDING_DTO,
            ttl
        ));


    }


    @Test
    void shouldUpdateCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

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


    @Test
    void willHandleExceptionGracefully() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder().id(CASE_ID).build())
            .build();

        doReturn(startEventResponse).when(coreCaseDataApi).startEvent(
            USER_TOKEN, SERVICE_TOKEN, String.valueOf(CASE_ID), ADD_RECORDING_FILE
        );

        JsonNode data = JsonNodeFactory.instance.objectNode();

        doReturn(data).when(caseDataContentCreator)
            .createCaseUpdateData(startEventResponse.getCaseDetails().getData(), RECORDING_ID, HEARING_RECORDING_DTO);

        CaseDataContent caseData = CaseDataContent.builder().data(data)
            .event(Event.builder().build()).build();


        doThrow(RuntimeException.class).when(coreCaseDataApi).submitEventForCaseWorker(
            USER_TOKEN, SERVICE_TOKEN, USER_ID,
            JURISDICTION, CASE_TYPE, String.valueOf(CASE_ID), false, caseData
        );

        assertThatExceptionOfType(CcdUploadException.class).isThrownBy(() -> underTest.updateCaseData(
            CASE_ID,
            RECORDING_ID,
            HEARING_RECORDING_DTO
        ));

    }

    @Test
    void willHandleExceptionGracefullyWhenCaseDataIsNullInUpdateCase() {
        doReturn(Map.of("user", USER_TOKEN,
                        "userId", USER_ID,
                        "service", SERVICE_TOKEN
        )).when(securityService).createTokens();

        doThrow(new RuntimeException("Failed to start event"))
            .when(coreCaseDataApi).startEvent(
                USER_TOKEN,
                SERVICE_TOKEN,
                String.valueOf(CASE_ID),
                ADD_RECORDING_FILE
            );

        assertThatExceptionOfType(CcdUploadException.class)
            .isThrownBy(() -> underTest.updateCaseData(
                CASE_ID,
                RECORDING_ID,
                HEARING_RECORDING_DTO
            ))
            .withMessage("Error Uploading Segment")
            .withCauseInstanceOf(RuntimeException.class);

        verify(caseDataContentCreator, never())
            .createCaseUpdateData(any(), any(), any());
        verify(coreCaseDataApi, never())
            .submitEventForCaseWorker(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldUpdateCaseWithTtlSuccessfully() {
        Long ccdCaseId = 123L;
        LocalDate recordingDate = LocalDate.now().minusDays(10);
        final LocalDate calculatedTtl = LocalDate.now().plusYears(7);

        final Map<String, String> tokens = Map.of(
            "user", USER_TOKEN,
            "service", SERVICE_TOKEN,
            "userId", USER_ID);

        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setRecordingDate(recordingDate);
        caseHearingRecording.setServiceCode("AAA7");
        caseHearingRecording.setJurisdictionCode("EMPLOYMENT");

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder()
                             .id(ccdCaseId)
                             .data(Map.of(
                                 "recordingDate", recordingDate.toString(),
                                 "serviceCode", "AAA7",
                                 "jurisdictionCode", "EMPLOYMENT"
                             ))
                             .build())
            .eventId("eventId")
            .token("eventToken")
            .build();

        TtlCcdObject ttlObject = new TtlCcdObject();

        doReturn(tokens).when(securityService).createTokens();
        doReturn(startEventResponse).when(coreCaseDataApi)
            .startEvent(USER_TOKEN, SERVICE_TOKEN, ccdCaseId.toString(), AMEND_CASE);
        when(ttlService.createTtl("AAA7", "EMPLOYMENT", recordingDate))
            .thenReturn(calculatedTtl);
        doReturn(ttlObject).when(caseDataContentCreator).createTTLObject(calculatedTtl);

        LocalDate result = underTest.updateCaseWithTtl(ccdCaseId);

        assertEquals(calculatedTtl, result);
        verify(ttlService).createTtl("AAA7", "EMPLOYMENT", recordingDate);
        verify(caseDataContentCreator).createTTLObject(calculatedTtl);
        verify(coreCaseDataApi).submitEventForCaseWorker(
            eq(USER_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID),
            eq(JURISDICTION), eq(CASE_TYPE), eq(ccdCaseId.toString()),
            eq(false), any(CaseDataContent.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenRecordingDateIsNull() {
        Long ccdCaseId = 123L;

        Map<String, String> tokens = Map.of(
            "user", USER_TOKEN,
            "service", SERVICE_TOKEN,
            "userId", USER_ID);

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder()
                             .id(ccdCaseId)
                             .data(Map.of(
                                 "serviceCode", "AAA7",
                                 "jurisdictionCode", "EMPLOYMENT"
                                 // recordingDate is missing/null
                             ))
                             .build())
            .eventId("eventId")
            .token("eventToken")
            .build();

        doReturn(tokens).when(securityService).createTokens();
        doReturn(startEventResponse).when(coreCaseDataApi)
            .startEvent(USER_TOKEN, SERVICE_TOKEN, ccdCaseId.toString(), AMEND_CASE);

        assertThatExceptionOfType(CcdUploadException.class)
            .isThrownBy(() -> underTest.updateCaseWithTtl(ccdCaseId))
            .withMessage("Error Updating TTL")
            .withCauseInstanceOf(IllegalStateException.class);

        verify(ttlService, never()).createTtl(any(), any(), any());
        verify(coreCaseDataApi, never()).submitEventForCaseWorker(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any()
        );
    }

    @Test
    void shouldHandleExceptionDuringUpdateCaseWithTtl() {
        Long ccdCaseId = 123L;
        LocalDate recordingDate = LocalDate.now().minusDays(10);
        final LocalDate calculatedTtl = LocalDate.now().plusYears(7);

        final Map<String, String> tokens = Map.of(
            "user", USER_TOKEN,
            "service", SERVICE_TOKEN,
            "userId", USER_ID);

        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setRecordingDate(recordingDate);
        caseHearingRecording.setServiceCode("AAA7");
        caseHearingRecording.setJurisdictionCode("EMPLOYMENT");

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder()
                             .id(ccdCaseId)
                             .data(Map.of(
                                 "recordingDate", recordingDate.toString(),
                                 "serviceCode", "AAA7",
                                 "jurisdictionCode", "EMPLOYMENT"
                             ))
                             .build())
            .eventId("eventId")
            .token("eventToken")
            .build();

        doReturn(tokens).when(securityService).createTokens();
        doReturn(startEventResponse).when(coreCaseDataApi)
            .startEvent(USER_TOKEN, SERVICE_TOKEN, ccdCaseId.toString(), AMEND_CASE);
        when(ttlService.createTtl("AAA7", "EMPLOYMENT", recordingDate))
            .thenReturn(calculatedTtl);
        doThrow(new RuntimeException("CCD update failed")).when(coreCaseDataApi)
            .submitEventForCaseWorker(any(), any(), any(), any(), any(), any(), anyBoolean(), any());

        assertThatExceptionOfType(CcdUploadException.class)
            .isThrownBy(() -> underTest.updateCaseWithTtl(ccdCaseId));
    }


    @Test
    void shouldUpdateCaseWithCodesSuccessfully() {
        Long ccdCaseId = 123L;
        Map<String, String> tokens = Map.of(
            "user", USER_TOKEN,
            "service", SERVICE_TOKEN,
            "userId", USER_ID);

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(CaseDetails.builder().id(ccdCaseId).data(Map.of()).build())
            .eventId("eventId")
            .token("eventToken")
            .build();

        doReturn(tokens).when(securityService).createTokens();
        doReturn(startEventResponse).when(coreCaseDataApi)
            .startEvent(USER_TOKEN, SERVICE_TOKEN, ccdCaseId.toString(), AMEND_CASE);

        underTest.updateCaseWithCodes(ccdCaseId, "jurisdiction code", "service codes");

        verify(coreCaseDataApi).submitEventForCaseWorker(eq(USER_TOKEN), eq(SERVICE_TOKEN), eq(USER_ID),
                                                         eq(JURISDICTION), eq(CASE_TYPE), eq(ccdCaseId.toString()),
                                                         eq(false), any(CaseDataContent.class));
    }

}
