package uk.gov.hmcts.reform.em.hrs.job;

import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.update-ttl.enabled", havingValue = "true")
public class UpdateTtlTask implements Runnable {

    private static final String TASK_NAME = "update-ttl";
    private static final Logger logger = getLogger(UpdateTtlTask.class);

    private final HearingRecordingService hearingRecordingService;
    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final TtlService ttlService;

    @Value("${scheduling.task.update-ttl.batch-size:50}")
    private int batchSize;

    @Value("${scheduling.task.update-ttl.thread-limit:10}")
    private int defaultThreadLimit;

    @Value("${scheduling.task.update-ttl.max-records:10000}")
    private int maxRecordsToProcess;

    public UpdateTtlTask(HearingRecordingService hearingRecordingService,
                         CcdDataStoreApiClient ccdDataStoreApiClient,
                         TtlService ttlService) {
        this.hearingRecordingService = hearingRecordingService;
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.ttlService = ttlService;
    }

    @Override
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<HearingRecordingTtlMigrationDTO> recordsToUpdate =
            hearingRecordingService.getRecordingsForTtlUpdate(maxRecordsToProcess);

        if (recordsToUpdate.isEmpty()) {
            logger.info("No records found requiring TTL update.");
            return;
        }

        logger.info("Found {} records to process", recordsToUpdate.size());

        List<List<HearingRecordingTtlMigrationDTO>> batches = Lists.partition(recordsToUpdate, batchSize);

        Set<Long> processedCcdCaseIds = ConcurrentHashMap.newKeySet();

        try (ExecutorService executorService = Executors.newFixedThreadPool(defaultThreadLimit)) {
            for (List<HearingRecordingTtlMigrationDTO> batch : batches) {
                executorService.submit(() -> processBatch(batch, processedCcdCaseIds));
            }
        } catch (Exception e) {
            logger.error("Error in executor service during {} task", TASK_NAME, e);
        }

        stopWatch.stop();
        logger.info("Finished {} job. Took {} ms", TASK_NAME, stopWatch.getDuration().toMillis());
    }

    private void processBatch(List<HearingRecordingTtlMigrationDTO> batch, Set<Long> processedCcdCaseIds) {
        for (HearingRecordingTtlMigrationDTO recording : batch) {
            try {
                processRecording(recording, processedCcdCaseIds);
            } catch (Exception e) {
                logger.error("Failed to process recording ID: {}", recording.id(), e);
            }
        }
    }

    private void processRecording(HearingRecordingTtlMigrationDTO recording, Set<Long> processedCcdCaseIds) {
        Long ccdCaseId = recording.ccdCaseId();

        LocalDate ttl = ttlService.createTtl(
            recording.serviceCode(),
            recording.jurisdictionCode(),
            LocalDate.from(recording.createdOn())
        );

        boolean ccdUpdateRequired = Objects.nonNull(ccdCaseId) && processedCcdCaseIds.add(ccdCaseId);

        if (ccdUpdateRequired) {
            try {
                ccdDataStoreApiClient.updateCaseWithTtl(ccdCaseId, ttl);
                logger.info("CCD updated for ccd case id: {}", ccdCaseId);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to update CCD for case id: " + ccdCaseId, e);
                //Don't proceed to update HRS if CCD update fails
            }
        }

        hearingRecordingService.updateTtl(recording.id(), ttl);
    }
}
