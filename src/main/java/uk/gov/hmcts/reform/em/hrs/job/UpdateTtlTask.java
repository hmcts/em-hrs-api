package uk.gov.hmcts.reform.em.hrs.job;

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

    @Value("${scheduling.task.update-ttl.no-of-iterations}")
    private int noOfIterations;

    @Value("${scheduling.task.update-ttl.thread-limit}")
    private int defaultThreadLimit;

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

        Map<Long, LocalDate> ttlByCcdCaseId = new ConcurrentHashMap<>();

        for (int i = 0; i < noOfIterations; i++) {
            StopWatch iterationStopWatch = new StopWatch();
            iterationStopWatch.start();

            StopWatch hrsGetQueryStopWatch = new StopWatch();
            hrsGetQueryStopWatch.start();

            List<HearingRecordingTtlMigrationDTO> recordsToUpdate =
                hearingRecordingService.getRecordingsForTtlUpdate(batchSize);

            hrsGetQueryStopWatch.stop();
            logger.info("Time taken to get {} rows from DB : {} ms", recordsToUpdate.size(),
                        hrsGetQueryStopWatch.getDuration().toMillis());

            if (recordsToUpdate.isEmpty()) {
                iterationStopWatch.stop();
                logger.info(
                    "No records found requiring TTL update. Time taken to complete iteration number : {} was : {} ms",
                    i, iterationStopWatch.getDuration().toMillis()
                );
                break;
            }

            try (ExecutorService executorService = Executors.newFixedThreadPool(defaultThreadLimit)) {
                for (HearingRecordingTtlMigrationDTO recording : recordsToUpdate) {
                    executorService.submit(() -> {
                        try {
                            processRecording(recording, ttlByCcdCaseId);
                        } catch (Exception e) {
                            logger.error("Failed to process recording ID: {}", recording.id(), e);
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error in executor service during {} task", TASK_NAME, e);
            }

            iterationStopWatch.stop();
            logger.info("Time taken to complete iteration number : {} was : {} ms", i,
                        iterationStopWatch.getDuration().toMillis());
        }

        stopWatch.stop();
        logger.info("Finished {} job. Took {} ms", TASK_NAME, stopWatch.getDuration().toMillis());
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
