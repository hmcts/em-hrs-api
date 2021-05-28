package uk.gov.hmcts.reform.em.hrs.service.ccd;

//import com.github.rholder.retry.Retryer;
//import com.github.rholder.retry.RetryerBuilder;
//import com.github.rholder.retry.StopStrategies;
//import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
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
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Service
public class CcdDataStoreApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcdDataStoreApiClient.class);
    private static final String JURISDICTION = "HRS";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final String CREATE_CASE = "createCase";
    private static final String ADD_RECORDING_FILE = "manageFiles";
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

    public Long createCase(final UUID recordingId, final HearingRecordingDto hearingRecordingDto) {
        Map<String, String> tokens = securityService.getTokens();

        StartEventResponse startEventResponse =
            coreCaseDataApi.startCase(tokens.get("user"), tokens.get("service"), CASE_TYPE, CREATE_CASE);

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(caseDataCreator.createCaseStartData(hearingRecordingDto, recordingId))
            .build();

        CaseDetails caseDetails = coreCaseDataApi
            .submitForCaseworker(tokens.get("user"), tokens.get("service"), tokens.get("userId"),
                                 JURISDICTION, CASE_TYPE, false, caseData
            );

        LOGGER.info("created a new case({}) for recording ({})",
                    caseDetails.getId(), hearingRecordingDto.getRecordingRef()
        );
        return caseDetails.getId();
    }


    public synchronized Long updateCaseData(final Long caseId, final UUID recordingId,
                                            final HearingRecordingDto hearingRecordingDto) {
        Map<String, String> tokens = securityService.getTokens();

        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(tokens.get("user"), tokens.get("service"),
                                                                           caseId.toString(), ADD_RECORDING_FILE
        );

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(caseDataCreator.createCaseUpdateData(
                startEventResponse.getCaseDetails().getData(), recordingId, hearingRecordingDto)
            ).build();

        LOGGER.info(
            "updating ccd case (id {}) with new recording (ref {})",
            caseId,
            hearingRecordingDto.getRecordingRef()
        );

        Long caseDetailsId = null;

        Callable<Long> callable = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return coreCaseDataApi
                    .submitEventForCaseWorker(tokens.get("user"), tokens.get("service"), tokens.get("userId"),
                                              JURISDICTION, CASE_TYPE, caseId.toString(), false, caseData
                    )
                    .getId();

            }
        };

        Retryer<Long> retryer = RetryerBuilder.<Long>newBuilder()
            .retryIfResult(Predicates.<Long>isNull())
            .retryIfExceptionOfType(IOException.class)//TODO determine the expected CCD exception thrown here
            .retryIfRuntimeException()
            .withWaitStrategy(WaitStrategies.fibonacciWait(2000, 2, TimeUnit.MINUTES))
            .withStopStrategy(StopStrategies.stopAfterAttempt(5))
            .build();
        try {
            caseDetailsId = retryer.call(callable);
        } catch (Exception e) {
            throw new CcdUploadException("Failed to upload to CCD " + e.getMessage(), e);
        }

        return caseDetailsId;
    }
}


