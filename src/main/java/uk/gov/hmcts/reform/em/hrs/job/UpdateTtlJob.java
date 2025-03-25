package uk.gov.hmcts.reform.em.hrs.job;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class UpdateTtlJob implements Runnable {

    private static final String TASK_NAME = "update-ttl";
    private static final Logger logger = getLogger(UpdateTtlJob.class);

    private final TtlService ttlService;
    private final HearingRecordingRepository hearingRecordingRepository;
    private final CcdDataStoreApiClient ccdDataStoreApiClient;

    @Value("${scheduling.task.update-ttl.batch-size}")
    private int batchSize;

    @Value("${scheduling.task.update-ttl.thread-limit}")
    private int threadLimit;

    public UpdateTtlJob(TtlService ttlService,
                        HearingRecordingRepository hearingRecordingRepository,
                        CcdDataStoreApiClient ccdDataStoreApiClient) {
        this.ttlService = ttlService;
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
    }

    public void run() {
        logger.info("Started {} job", TASK_NAME);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<HearingRecording> recordingsWithoutTtl =
            hearingRecordingRepository.findByTtlSetFalseOrderByCreatedOnAsc(Limit.of(batchSize));

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadLimit)) {
            for (HearingRecording recording : recordingsWithoutTtl) {
                LocalDate ttl = ttlService.createTtl(recording.getServiceCode(), recording.getJurisdictionCode(),
                                                     LocalDate.from(recording.getCreatedOn())
                );

                executorService.submit(() -> processRecording(recording, ttl));
            }
        }

        stopWatch.stop();
        logger.info("Update job for ttl took {} ms", stopWatch.getDuration().toMillis());

        logger.info("Finished {} job", TASK_NAME);
    }

    private void processRecording(HearingRecording recording, LocalDate ttl) {
        try {
            Long ccdCaseId = recording.getCcdCaseId();
            logger.info("Updating case with ttl for recording id: {}, caseId: {}", recording.getId(), ccdCaseId);
            ccdDataStoreApiClient.updateCaseWithTtl(ccdCaseId, ttl);
        } catch (Exception e) {
            logger.info("Failed to update case with ttl for recording id: {}, caseId: {}",
                        recording.getId(), recording.getCcdCaseId(), e);
            return;
        }

        updateRecordingTtl(recording, ttl);
    }

    private void updateRecordingTtl(HearingRecording recording, LocalDate ttl) {
        Long ccdCaseId = recording.getCcdCaseId();
        logger.info("Updating recording ttl for recording id: {}, caseId: {}", recording.getId(), ccdCaseId);
        try {
            recording.setTtlSet(true);
            recording.setTtl(ttl);
            hearingRecordingRepository.save(recording);
        } catch (Exception e) {
            logger.error("Failed to update recording ttl for recording id: {}, caseId: {}",
                         recording.getId(), ccdCaseId, e);
        }
    }
}
