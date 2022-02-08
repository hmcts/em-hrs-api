package uk.gov.hmcts.reform.em.hrs.service.email;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.summary-report.enabled")
public class SummaryReportTask {

    private static final String TASK_NAME = "create-summary-report";
    private static final Logger logger = getLogger(SummaryReportTask.class);

    private final SummaryReportService summaryReportService;

    public SummaryReportTask(SummaryReportService summaryReportService) {
        this.summaryReportService = summaryReportService;
    }

    @Scheduled(cron = "${scheduling.task.summary-report.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        summaryReportService.sendReport();

        logger.info("Finished {} job", TASK_NAME);

    }
}
