package uk.gov.hmcts.reform.em.hrs.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-vh-recordings.enabled")
public class DeleteVhRecordingTask {

    private static final String TASK_NAME = "delete-vh-recordings";
    private static final Logger logger = getLogger(DeleteVhRecordingTask.class);

    private final HearingRecordingRepository hearingRecordingRepository;
    private final ShareesRepository shareesRepository;

    private final ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository;

    public DeleteVhRecordingTask(
        HearingRecordingRepository hearingRecordingRepository,
        ShareesRepository shareesRepository,
        ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository
    ) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.shareesRepository = shareesRepository;
        this.hearingRecordingShareeAuditEntryRepository = hearingRecordingShareeAuditEntryRepository;
    }

    @Scheduled(cron = "${scheduling.task.delete-vh-recordings.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        List<UUID> recordsToDelete = hearingRecordingRepository.listVhRecordingsToDelete();
        for (var id : recordsToDelete) {
            var hearingRecOpt = hearingRecordingRepository.findById(id);
            if (hearingRecOpt.isEmpty()) {
                return;
            }
            var hearingRec = hearingRecOpt.get();
            hearingRecordingShareeAuditEntryRepository.deleteByCaseRef(hearingRec.getCcdCaseId());
            logger.info("shareesRepository Deleting id {} ", id);
            shareesRepository.deleteByHearingRecordingId(id);
            logger.info("sharee deleted for id {} ", id);
            hearingRecordingRepository.deleteById(id);
            logger.info("Deleted id {} ", id);
        }
        logger.info("Finished {} job,record count {}", TASK_NAME, recordsToDelete);
    }
}
