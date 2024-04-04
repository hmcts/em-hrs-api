package uk.gov.hmcts.reform.em.hrs.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-vh-blobs.enabled")
public class DeleteVhBlobsTask {

    private static final String TASK_NAME = "delete-vh-blob";
    private static final Logger logger = getLogger(DeleteVhBlobsTask.class);

    private final HearingRecordingStorage hearingRecordingStorage;

    public DeleteVhBlobsTask(HearingRecordingStorage hearingRecordingStorage) {
        this.hearingRecordingStorage = hearingRecordingStorage;
    }

    @Scheduled(cron = "${scheduling.task.delete-vh-blobs.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        hearingRecordingStorage.listVHBlobs();
        logger.info("Finished {} job", TASK_NAME);
    }
}
