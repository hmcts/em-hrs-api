package uk.gov.hmcts.reform.em.hrs.service.email;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class AuditReportService {
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
