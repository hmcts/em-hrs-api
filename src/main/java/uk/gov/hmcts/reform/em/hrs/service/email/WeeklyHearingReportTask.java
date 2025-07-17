package uk.gov.hmcts.reform.em.hrs.service.email;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.weekly-hearing-report.enabled")
public class WeeklyHearingReportTask {

    private static final String TASK_NAME = "create-weekly-hearing-report";
    private static final Logger logger = getLogger(WeeklyHearingReportTask.class);

    private final HearingReportEmailService hearingReportEmailService;
    private final HearingReportService hearingReportService;

    private final List<LocalDate> reportStartDateList;

    public WeeklyHearingReportTask(
        @Qualifier("weeklyHearingEmailService") HearingReportEmailService hearingReportEmailService,
        HearingReportService hearingReportService,
        @Value("#{dateListConverter.convert('${report.weekly-hearing.reportStartDates}')}")
        List<LocalDate> reportStartDates) {
        this.hearingReportEmailService = hearingReportEmailService;
        this.hearingReportService = hearingReportService;
        this.reportStartDateList = reportStartDates;
    }


    @Scheduled(cron = "${scheduling.task.weekly-hearing-report.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME, lockAtLeastFor = "PT10M")
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        if (reportStartDateList.isEmpty()) {
            reportStartDateList.add(LocalDate.now());
        }

        for (var reportStartDate : reportStartDateList) {
            try {
                logger.info("Starting report for {}", reportStartDate);
                var reportFile = hearingReportService.createWeeklyReport(reportStartDate);

                hearingReportEmailService.sendReport(reportStartDate, reportFile);
                logger.info("Finished report for {}", reportStartDate);
            } catch (Exception ex) {
                logger.error("Failed report for {}", reportStartDate, ex);
            }
        }

        logger.info("Finished {} job", TASK_NAME);

    }
}
