package uk.gov.hmcts.reform.em.hrs.job;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
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

        StopWatch hrsGetQueryStopWatch = new StopWatch();
        hrsGetQueryStopWatch.start();

        List<HearingRecordingTtlMigrationDTO> recordingsWithoutTtl =
            hearingRecordingRepository.findByTtlSetFalseOrderByCreatedOnAsc(PageRequest.of(0, batchSize));

        hrsGetQueryStopWatch.stop();
        logger.info("Time taken to get {} rows from DB : {} ms", batchSize,
                hrsGetQueryStopWatch.getDuration().toMillis());

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadLimit)) {
            for (HearingRecordingTtlMigrationDTO recording : recordingsWithoutTtl) {
                LocalDate ttl = ttlService.createTtl(recording.serviceCode(), recording.jurisdictionCode(),
                                                     LocalDate.from(recording.createdOn())
                );

                executorService.submit(() -> processRecording(recording, ttl));
            }
        }

        stopWatch.stop();
        logger.info("Update job for ttl took {} ms", stopWatch.getDuration().toMillis());

        logger.info("Finished {} job", TASK_NAME);
    }

    private void processRecording(HearingRecordingTtlMigrationDTO recording, LocalDate ttl) {

        StopWatch processRecordingStopWatch = new StopWatch();
        processRecordingStopWatch.start();

        Long ccdCaseId = recording.ccdCaseId();
        try {
            StopWatch ccdUpdateCallStopWatch = new StopWatch();
            ccdUpdateCallStopWatch.start();

            logger.info("Updating case with ttl for recording id: {}, caseId: {}", recording.id(), ccdCaseId);
            ccdDataStoreApiClient.updateCaseWithTtl(ccdCaseId, ttl);

            ccdUpdateCallStopWatch.stop();
            logger.info("Updating case in CCD with caseId:{} took : {} ms", ccdCaseId,
                    ccdUpdateCallStopWatch.getDuration().toMillis());

        } catch (Exception e) {
            logger.info("Failed to update case with ttl for recording id: {}, caseId: {}",
                        recording.id(), recording.ccdCaseId(), e);
            return;
        }

        updateRecordingTtl(recording, ttl);

        processRecordingStopWatch.stop();
        logger.info("Processing case with caseId:{} took : {} ms", ccdCaseId,
                processRecordingStopWatch.getDuration().toMillis());
    }

    private void updateRecordingTtl(HearingRecordingTtlMigrationDTO recordingDto, LocalDate ttl) {
        Long ccdCaseId = recordingDto.ccdCaseId();
        logger.info("Updating recording ttl for recording id: {}, caseId: {}", recordingDto.id(), ccdCaseId);
        try {
            StopWatch hrsUpdateDBStopWatch = new StopWatch();
            hrsUpdateDBStopWatch.start();

            HearingRecording recording = new HearingRecording();
            recording.setId(recordingDto.id());
            recording.setTtlSet(true);
            recording.setTtl(ttl);
            hearingRecordingRepository.save(recording);

            hrsUpdateDBStopWatch.stop();
            logger.info("Updating HRS details in HRS with caseId:{} took : {} ms", ccdCaseId,
                    hrsUpdateDBStopWatch.getDuration().toMillis());
        } catch (Exception e) {
            logger.info("Failed to update recording ttl for recording id: {}, caseId: {}",
                         recordingDto.id(), ccdCaseId, e);
        }
    }
}
