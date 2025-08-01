package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson;
import com.microsoft.applicationinsights.core.dependencies.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class CcdDataStoreApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcdDataStoreApiClient.class);
    private static final String JURISDICTION = "HRS";
    private static final String SERVICE = "service";
    private static final String USER = "user";
    private static final String USER_ID = "userId";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final String EVENT_CREATE_CASE = "createCase";
    private static final String EVENT_MANAGE_FILES = "manageFiles";
    private static final String EVENT_AMEND_CASE = "editCaseDetails";
    private final SecurityService securityService;
    private final CaseDataContentCreator caseDataCreator;
    private final CoreCaseDataApi coreCaseDataApi;

    public CcdDataStoreApiClient(SecurityService securityService,
                                 CaseDataContentCreator caseDataCreator,
                                 CoreCaseDataApi coreCaseDataApi) {
        this.securityService = securityService;
        this.caseDataCreator = caseDataCreator;
        this.coreCaseDataApi = coreCaseDataApi;
    }

    public Long createCase(
        final UUID recordingId,
        final HearingRecordingDto hearingRecordingDto,
        LocalDate ttl
    ) {
        CaseDataContent caseData = null;
        try {
            LOGGER.info("Starting Case Event");
            Map<String, String> tokens = securityService.createTokens();
            StartEventResponse startEventResponse =
                coreCaseDataApi.startCase(tokens.get(USER), tokens.get(SERVICE), CASE_TYPE, EVENT_CREATE_CASE);

            caseData = buildCaseDataContent(
                startEventResponse,
                caseDataCreator.createCaseStartData(hearingRecordingDto, recordingId, ttl)
            );

            CaseDetails caseDetails = coreCaseDataApi
                .submitForCaseworker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                     JURISDICTION, CASE_TYPE, false, caseData
                );

            LOGGER.info(
                "created a new case({}) for recording ({})",
                caseDetails.getId(),
                hearingRecordingDto.getRecordingRef()
            );
            return caseDetails.getId();

        } catch (Exception e) {
            //CCD has rejected, so log payload to assist with debugging (no sensitive information is exposed)
            if (caseData != null) {
                logCaseDataError(caseData);
            } else {
                LOGGER.error(
                    "caseReference: {}, eventSummary: {}",
                    hearingRecordingDto.getCaseRef(),
                    "Create New Case"
                );
            }
            throw new CcdUploadException("Error Creating Case", e);
        }
    }


    public synchronized Long updateCaseData(final Long caseId, final UUID recordingId,
                                            final HearingRecordingDto hearingRecordingDto) {
        CaseDataContent caseData = null;

        try {
            Map<String, String> tokens = securityService.createTokens();
            StartEventResponse startEventResponse =
                startEvent(tokens, caseId, EVENT_MANAGE_FILES);

            caseData = buildCaseDataContent(startEventResponse, caseDataCreator.createCaseUpdateData(
                startEventResponse.getCaseDetails().getData(), recordingId, hearingRecordingDto));

            LOGGER.info(
                "updating ccd case (id {}) with new recording (ref {})",
                caseId,
                hearingRecordingDto.getRecordingRef()
            );

            CaseDetails caseDetails =
                coreCaseDataApi.submitEventForCaseWorker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                                         JURISDICTION, CASE_TYPE, caseId.toString(), false, caseData
                );

            return caseDetails.getId();

        } catch (Exception e) {
            //CCD has rejected, so log payload to assist with debugging (no sensitive information is exposed)
            if (caseData != null) {
                logCaseDataError(caseData);
            } else {
                LOGGER.error(
                    "caseReference: {}, filename: {}, eventSummary: {}",
                    hearingRecordingDto.getCaseRef(),
                    hearingRecordingDto.getFilename(),
                    "Add Segment to Case"
                );
            }
            throw new CcdUploadException("Error Uploading Segment", e);
        }

    }

    public void updateCaseWithTtl(Long ccdCaseId, LocalDate ttl) {
        try {
            Map<String, String> tokens = securityService.createTokens();
            StartEventResponse startEventResponse = startEvent(tokens, ccdCaseId, EVENT_AMEND_CASE);

            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();

            CaseDetails caseDetails = startEventResponse.getCaseDetails();
            CaseHearingRecording caseHearingRecording = mapper.convertValue(
                caseDetails.getData(), CaseHearingRecording.class);
            TtlCcdObject ttlObject = caseDataCreator.createTTLObject(ttl);
            caseHearingRecording.setTimeToLive(ttlObject);

            CaseDataContent caseDataContent = buildCaseDataContent(
                startEventResponse, mapper.convertValue(caseHearingRecording, JsonNode.class));
            coreCaseDataApi.submitEventForCaseWorker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                                     JURISDICTION, CASE_TYPE, ccdCaseId.toString(),
                                                     false, caseDataContent);
        } catch (Exception e) {
            throw new CcdUploadException("Error Updating TTL", e);
        }
    }

    public void updateCaseWithCodes(Long ccdCaseId, String jurisdictionCode, String serviceCode) {

        Map<String, String> tokens = securityService.createTokens();
        StartEventResponse startEventResponse = startEvent(tokens, ccdCaseId, EVENT_AMEND_CASE);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        CaseDetails caseDetails = startEventResponse.getCaseDetails();
        CaseHearingRecording caseHearingRecording = mapper.convertValue(
            caseDetails.getData(), CaseHearingRecording.class);
        caseHearingRecording.setJurisdictionCode(jurisdictionCode);
        caseHearingRecording.setServiceCode(serviceCode);

        CaseDataContent caseDataContent = buildCaseDataContent(
            startEventResponse, mapper.convertValue(caseHearingRecording, JsonNode.class));
        coreCaseDataApi.submitEventForCaseWorker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                                 JURISDICTION, CASE_TYPE, ccdCaseId.toString(),
                                                 false, caseDataContent);

    }

    private StartEventResponse startEvent(Map<String, String> tokens, Long caseId, String eventType) {
        return coreCaseDataApi.startEvent(
            tokens.get(USER),
            tokens.get(SERVICE),
            caseId.toString(),
            eventType
        );
    }

    private CaseDataContent buildCaseDataContent(StartEventResponse startEventResponse, Object data) {
        return CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(data)
            .build();
    }

    private void logCaseDataError(CaseDataContent caseData) {
        String caseReference = caseData.getCaseReference();
        Event event = caseData.getEvent();
        String eventDescription = event.getDescription();
        String eventId = event.getId();
        String eventSummary = event.getSummary();

        LOGGER.error(
            "caseReference: {}, eventId: {}, eventDescription: {}, eventSummary: {}",
            caseReference,
            eventId,
            eventDescription,
            eventSummary
        );
        Object jsonData = caseData.getData();
        LOGGER.info("caseData Raw: {}", jsonData);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(jsonData);
        LOGGER.info("caseData Pretty: {}", jsonOutput);
    }

}


