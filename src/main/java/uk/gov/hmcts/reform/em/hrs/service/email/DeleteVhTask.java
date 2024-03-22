package uk.gov.hmcts.reform.em.hrs.service.email;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-vh-recordings.enabled")
public class DeleteVhTask {

    private static final String TASK_NAME = "delete-vh-recordings";
    private static final Logger logger = getLogger(DeleteVhTask.class);

    private final HearingRecordingRepository hearingRecordingRepository;

    public DeleteVhTask(HearingRecordingRepository hearingRecordingRepository) {
        this.hearingRecordingRepository = hearingRecordingRepository;
    }

    @Scheduled(cron = "${scheduling.delete-vh-recordings.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        int  num = hearingRecordingRepository.deleteVhRecordings();
        logger.info("Finished {} job, delete item count {}", TASK_NAME, num);
    }
}
