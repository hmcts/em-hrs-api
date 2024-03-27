package uk.gov.hmcts.reform.em.hrs.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-vh-recordings.enabled")
public class DeleteVhRecordingTask {

    private static final String TASK_NAME = "delete-vh-recordings";
    private static final Logger logger = getLogger(DeleteVhRecordingTask.class);

    private final HearingRecordingRepository hearingRecordingRepository;

    public DeleteVhRecordingTask(HearingRecordingRepository hearingRecordingRepository) {
        this.hearingRecordingRepository = hearingRecordingRepository;
    }

    @Scheduled(cron = "${scheduling.delete-vh-recordings.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        hearingRecordingRepository.deleteVhRecordings(UUID.fromString("e1d00616-d98a-41db-b2bf-4a9a836265fe"));
        logger.info("Finished {} job", TASK_NAME);
    }
}
