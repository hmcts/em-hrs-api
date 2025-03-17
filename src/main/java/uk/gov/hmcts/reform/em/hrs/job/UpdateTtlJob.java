package uk.gov.hmcts.reform.em.hrs.job;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
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
@ConditionalOnProperty(value = "scheduling.task.update-ttl.enabled")
public class UpdateTtlJob {

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

    @Scheduled(cron = "${scheduling.task.update-ttl.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

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

        logger.info("Finished {} job", TASK_NAME);
    }

    private void processRecording(HearingRecording recording, LocalDate ttl) {
        try {
            ccdDataStoreApiClient.updateCaseWithTtl(recording.getCcdCaseId(), ttl);
        } catch (Exception e) {
            logger.error("Failed to update case with ttl for recording id: {}", recording.getId(), e);
            return;
        }

        updateRecordingTtl(recording, ttl);
    }

    private void updateRecordingTtl(HearingRecording recording, LocalDate ttl) {
        try {
            recording.setTtlSet(true);
            recording.setTtl(ttl);
            hearingRecordingRepository.save(recording);
        } catch (Exception e) {
            logger.error("Failed to update recording ttl for recording id: {}", recording.getId(), e);
        }
    }
}
