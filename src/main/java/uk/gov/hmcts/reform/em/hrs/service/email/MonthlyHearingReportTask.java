package uk.gov.hmcts.reform.em.hrs.service.email;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.monthly-hearing-report.enabled")
public class MonthlyHearingReportTask {

    private static final String TASK_NAME = "create-monthly-hearing-report";
    private static final Logger logger = getLogger(MonthlyHearingReportTask.class);

    private final HearingReportEmailService hearingReportEmailService;

    public MonthlyHearingReportTask(HearingReportEmailService hearingReportEmailService) {
        this.hearingReportEmailService = hearingReportEmailService;
    }

    @Scheduled(cron = "${scheduling.task.monthly-hearing-report.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        hearingReportEmailService.sendReport();

        logger.info("Finished {} job", TASK_NAME);

    }
}
