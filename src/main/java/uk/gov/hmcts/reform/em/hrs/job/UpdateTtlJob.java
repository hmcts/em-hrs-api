package uk.gov.hmcts.reform.em.hrs.job;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${scheduling.task.update-ttl.batch-size}")
    private int batchSize;

    @Value("${scheduling.task.update-ttl.no-of-iterations}")
    private int noOfIterations;

    @Value("${scheduling.task.update-ttl.thread-limit}")
    private int threadLimit;

    public UpdateTtlJob(TtlService ttlService,
                        HearingRecordingRepository hearingRecordingRepository,
                        CcdDataStoreApiClient ccdDataStoreApiClient,
                        NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.ttlService = ttlService;
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public void run() {
        logger.info("Started {} job", TASK_NAME);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < noOfIterations; i++) {
            StopWatch iterationStopWatch = new StopWatch();
            iterationStopWatch.start();

            StopWatch hrsGetQueryStopWatch = new StopWatch();
            hrsGetQueryStopWatch.start();

            List<HearingRecordingTtlMigrationDTO> recordingsWithoutTtl =
                    hearingRecordingRepository.findByTtlSetFalseOrderByCreatedOnAsc(PageRequest.of(0, batchSize));

            hrsGetQueryStopWatch.stop();
            logger.info("Time taken to get {} rows from DB : {} ms", recordingsWithoutTtl.size(),
                    hrsGetQueryStopWatch.getDuration().toMillis());

            if (CollectionUtils.isEmpty(recordingsWithoutTtl)) {
                iterationStopWatch.stop();
                logger.info("Time taken to complete iteration number :  {} was : {} ms", i,
                        hrsGetQueryStopWatch.getDuration().toMillis());
                break;
            }
            try (ExecutorService executorService = Executors.newFixedThreadPool(threadLimit)) {
                for (HearingRecordingTtlMigrationDTO recording : recordingsWithoutTtl) {
                    LocalDate ttl = ttlService.createTtl(recording.serviceCode(), recording.jurisdictionCode(),
                            LocalDate.from(recording.createdOn())
                    );

                    executorService.submit(() -> processRecording(recording, ttl));
                }
            }
            iterationStopWatch.stop();
            logger.info("Time taken to complete iteration number :  {} was : {} ms", i,
                    hrsGetQueryStopWatch.getDuration().toMillis());
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

            updateHrsMetaData(new UpdateRecordingRecord(recordingDto.id(), true,ttl));

            hrsUpdateDBStopWatch.stop();
            logger.info("Updating HRS details in HRS with caseId:{} took : {} ms", ccdCaseId,
                    hrsUpdateDBStopWatch.getDuration().toMillis());
        } catch (Exception e) {
            logger.info("Failed to update recording ttl for recording id: {}, caseId: {}",
                         recordingDto.id(), ccdCaseId, e);
        }
    }

    private void updateHrsMetaData(UpdateRecordingRecord updateRecordingRecord) {
        // This maps the object's properties to the named parameters in the SQL
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(updateRecordingRecord);
        namedParameterJdbcTemplate.update("""
            UPDATE hearing_recording SET ttl_set = :ttlSet, ttl = :ttl WHERE id = :id
            """, params);
    }

    private record UpdateRecordingRecord(UUID id, boolean ttlSet, LocalDate ttl){}
}
