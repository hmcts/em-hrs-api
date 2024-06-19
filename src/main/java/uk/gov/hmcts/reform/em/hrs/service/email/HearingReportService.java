package uk.gov.hmcts.reform.em.hrs.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;

@Service
public class HearingReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingReportService.class);


    private final HearingRecordingSegmentRepository hearingRecordingSegmentRepository;
    private final HearingReportCsvWriter hearingReportCsvWriter;

    public HearingReportService(
        HearingRecordingSegmentRepository hearingRecordingSegmentRepository,
        HearingReportCsvWriter hearingReportCsvWriter
    ) {
        this.hearingRecordingSegmentRepository = hearingRecordingSegmentRepository;
        this.hearingReportCsvWriter = hearingReportCsvWriter;
    }

    public File createMonthlyReport(Month month, int year) throws IOException {
        LocalDateTime startOfMonth = getStartOfMonth(month, year);
        LocalDateTime endOfMonth = getEndOfMonth(month, year);
        LOGGER.info("get records for from: {},to:{}", startOfMonth, endOfMonth);
        var list = hearingRecordingSegmentRepository.findByCreatedOnBetweenDates(startOfMonth, endOfMonth);
        return hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(list);
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
