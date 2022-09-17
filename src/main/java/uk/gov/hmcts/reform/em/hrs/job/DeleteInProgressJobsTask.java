package uk.gov.hmcts.reform.em.hrs.job;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-inprogress-jobs.enabled")
public class DeleteInProgressJobsTask {

    private static final String TASK_NAME = "delete-inprogress-jobs";
    private static final Logger logger = getLogger(DeleteInProgressJobsTask.class);

    private final HearingRecordingRepository hearingRecordingRepository;
    private final JobInProgressRepository jobInProgressRepository;
    private final int ttlHours;

    public DeleteInProgressJobsTask(
        HearingRecordingRepository hearingRecordingRepository,
        JobInProgressRepository jobInProgressRepository,
        @Value("${scheduling.task.delete-inprogress-jobs.ttl-hours}") int ttlHours
    ) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.jobInProgressRepository = jobInProgressRepository;
        this.ttlHours = ttlHours;
    }

    @Scheduled(cron = "${scheduling.task.delete-inprogress-jobs.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        LocalDateTime hoursAgo = LocalDateTime.now(Clock.systemUTC()).minusHours(ttlHours);
        try {
            jobInProgressRepository.deleteByCreatedOnLessThan(hoursAgo);
            hearingRecordingRepository.deleteStaleRecordsWithNullCcdCaseId(hoursAgo);
        } catch (Exception ex) {
            logger.info("ERROR in {} job", TASK_NAME, ex);
        }
        logger.info("Finished {} job", TASK_NAME);
    }
}
