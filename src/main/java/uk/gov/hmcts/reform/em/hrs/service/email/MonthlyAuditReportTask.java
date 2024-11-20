package uk.gov.hmcts.reform.em.hrs.service.email;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.monthly-hearing-audit-report.enabled")
public class MonthlyAuditReportTask {

    private static final String TASK_NAME = "create-monthly-hearing-audit-report";
    private static final Logger logger = getLogger(MonthlyAuditReportTask.class);

    private final HearingReportEmailService hearingReportEmailService;

    private final AuditReportService monthlyReportService;

    private final List<LocalDate> reportStartDateList;

    public MonthlyAuditReportTask(
        HearingReportEmailService hearingReportEmailService,
        AuditReportService monthlyReportService,
        @Value("#{dateListConverter.convert('${report.monthly-hearing-audit.reportStartDates}')}")
        List<LocalDate> reportStartDates) {
        this.hearingReportEmailService = hearingReportEmailService;
        this.monthlyReportService = monthlyReportService;
        this.reportStartDateList = reportStartDates;
    }


    @Scheduled(cron = "${scheduling.task.monthly-hearing-audit-report.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        if (reportStartDateList.isEmpty()) {
            reportStartDateList.add(LocalDate.now().minusMonths(1));
        }

        for (var reportStartDate : reportStartDateList) {
            try {
                logger.info("Starting report for {}", reportStartDate);

                var reportFile = monthlyReportService.createMonthlyReport(
                    reportStartDate.getMonth(),
                    reportStartDate.getYear()
                );

                hearingReportEmailService.sendReport(reportStartDate, reportFile);
                logger.info("Finished report for {}", reportStartDate);
            } catch (Exception ex) {
                logger.error("Failed report for {}", reportStartDate, ex);
            }
        }

        logger.info("Finished {} job", TASK_NAME);

    }

    @Service
    public static class AuditReportService {
        private static final Logger LOGGER = getLogger(AuditReportService.class);

        private final AuditEntryService auditEntryService;
        private final AuditReportCsvWriter auditReportCsvWriter;

        public AuditReportService(
            AuditEntryService auditEntryService,
            AuditReportCsvWriter auditReportCsvWriter
        ) {
            this.auditEntryService = auditEntryService;
            this.auditReportCsvWriter = auditReportCsvWriter;
        }

        @Transactional
        public File createMonthlyReport(Month month, int year) throws IOException {
            LocalDateTime startOfMonth = getStartOfMonth(month, year);
            LocalDateTime endOfMonth = getEndOfMonth(month, year);
            LOGGER.info("get records for from: {},to:{}", startOfMonth, endOfMonth);
            var list = auditEntryService.listHearingRecordingAudits(startOfMonth, endOfMonth);
            return auditReportCsvWriter.writeHearingRecordingSummaryToCsv(list);
        }

        private LocalDateTime getStartOfMonth(Month month, int year) {
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            return firstDayOfMonth.atStartOfDay();
        }

        private LocalDateTime getEndOfMonth(Month month, int year) {
            LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();
            return lastDayOfMonth.atTime(23, 59, 59);
        }
    }
}
