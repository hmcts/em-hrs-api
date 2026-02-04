package uk.gov.hmcts.reform.em.hrs.job;

import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class UpdateTtlTask implements Runnable {

    private static final String TASK_NAME = "update-ttl";
    private static final Logger logger = getLogger(UpdateTtlTask.class);

    private final HearingRecordingService hearingRecordingService;
    private final CcdDataStoreApiClient ccdDataStoreApiClient;

    @Value("${scheduling.task.update-ttl.batch-size}")
    private int batchSize;

    @Value("${scheduling.task.update-ttl.thread-limit}")
    private int defaultThreadLimit;

    @Value("${scheduling.task.update-ttl.max-records}")
    private int maxRecordsToProcess;

    public UpdateTtlTask(HearingRecordingService hearingRecordingService,
                         CcdDataStoreApiClient ccdDataStoreApiClient) {
        this.hearingRecordingService = hearingRecordingService;
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
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

        // Cache TTL per CCD case id. Ensures CCD is updated once per case id and HRS is updated for every record.
        Map<Long, LocalDate> ttlByCcdCaseId = new ConcurrentHashMap<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(defaultThreadLimit)) {
            for (List<HearingRecordingTtlMigrationDTO> batch : batches) {
                executorService.submit(() -> processBatch(batch, ttlByCcdCaseId));
            }
        } catch (Exception e) {
            logger.error("Error in executor service during {} task", TASK_NAME, e);
        }

        stopWatch.stop();
        logger.info("Finished {} job. Took {} ms", TASK_NAME, stopWatch.getDuration().toMillis());
    }

    private void processBatch(List<HearingRecordingTtlMigrationDTO> batch,
                              Map<Long, LocalDate> ttlByCcdCaseId) {
        for (HearingRecordingTtlMigrationDTO recording : batch) {
            try {
                processRecording(recording, ttlByCcdCaseId);
            } catch (Exception e) {
                logger.error("Failed to process recording ID: {}", recording.id(), e);
            }
        }
    }

    private void processRecording(HearingRecordingTtlMigrationDTO recording,
                                  Map<Long, LocalDate> ttlByCcdCaseId) {

        Long ccdCaseId = recording.ccdCaseId();
        if (Objects.isNull(ccdCaseId)) {
            throw new IllegalStateException("Missing ccdCaseId for recording id: " + recording.id());
        }

        // Update CCD only once per ccdCaseId and cache the TTL used.
        // HRS is always updated for each recording using the cached TTL.
        LocalDate ttl = ttlByCcdCaseId.computeIfAbsent(ccdCaseId, id -> {
            try {
                LocalDate computedTtl = ccdDataStoreApiClient.updateCaseWithTtl(id);
                logger.info("CCD updated for ccd case id: {}", id);
                return computedTtl;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to update CCD for case id: " + id, e);
                // Don't proceed to update HRS if CCD update fails
            }
        });

        hearingRecordingService.updateTtl(recording.id(), ttl);
    }
}
