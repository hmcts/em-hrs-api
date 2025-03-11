package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingReportServiceTest {

    @Mock
    private HearingRecordingSegmentRepository hearingRecordingSegmentRepository;

    @Mock
    private HearingReportCsvWriter hearingReportCsvWriter;

    @InjectMocks
    private HearingReportService hearingReportService;

    private List<HearingRecordingSegment> expectedRecords;

    @BeforeEach
    void setUp() {
        expectedRecords = Arrays.asList(
            new HearingRecordingSegment(),
            new HearingRecordingSegment(),
            new HearingRecordingSegment()
        );
    }

    @Test
    void should_get_monthly_report() throws IOException {
        Month month = Month.JUNE;
        int year = 2023;
        LocalDateTime startOfMonth = LocalDateTime.of(2023, 6, 1, 0, 0, 0);
        LocalDateTime endOfMonth = LocalDateTime.of(2023, 6, 30, 23, 59, 59);

        when(hearingRecordingSegmentRepository
                 .findByCreatedOnBetweenDatesWithHearingRecording(startOfMonth, endOfMonth))
            .thenReturn(expectedRecords);

        when(hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(expectedRecords))
            .thenReturn(new File("temp"));

        File csvFile = hearingReportService.createMonthlyReport(month, year);
        assertNotNull(csvFile);
        verify(hearingRecordingSegmentRepository, times(1))
            .findByCreatedOnBetweenDatesWithHearingRecording(startOfMonth, endOfMonth);
        verify(hearingReportCsvWriter, times(1))
            .writeHearingRecordingSummaryToCsv(expectedRecords);
    }

    @Test
    void should_get_monthly_report_throws_same_exception() throws IOException {
        Month month = Month.JUNE;
        int year = 2023;
        LocalDateTime startOfMonth = LocalDateTime.of(2023, 6, 1, 0, 0, 0);
        LocalDateTime endOfMonth = LocalDateTime.of(2023, 6, 30, 23, 59, 59);

        when(hearingRecordingSegmentRepository
                 .findByCreatedOnBetweenDatesWithHearingRecording(startOfMonth, endOfMonth))
            .thenThrow(new RuntimeException("Dummy error"));

        try {
            hearingReportService.createMonthlyReport(month, year);
        } catch (RuntimeException exception) {
            assertEquals("Dummy error", exception.getMessage());
        }
        verify(hearingRecordingSegmentRepository, times(1))
            .findByCreatedOnBetweenDatesWithHearingRecording(startOfMonth, endOfMonth);
    }

    @Test
    void should_get_weekly_report() throws IOException {
        LocalDate reportDate = LocalDate.of(2024, 2, 20); // Example Tuesday
        LocalDate startOfWeek = reportDate.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDateTime = endOfWeek.atTime(23, 59, 59);

        when(hearingRecordingSegmentRepository
                 .findByCreatedOnBetweenDatesWithHearingRecording(startOfWeekDateTime, endOfWeekDateTime))
            .thenReturn(expectedRecords);

        when(hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(expectedRecords))
            .thenReturn(new File("temp"));

        File csvFile = hearingReportService.createWeeklyReport(reportDate);
        assertNotNull(csvFile);
        verify(hearingRecordingSegmentRepository, times(1))
            .findByCreatedOnBetweenDatesWithHearingRecording(startOfWeekDateTime, endOfWeekDateTime);
        verify(hearingReportCsvWriter, times(1))
            .writeHearingRecordingSummaryToCsv(expectedRecords);
    }

    @Test
    void should_get_weekly_report_throws_same_exception() throws IOException {
        LocalDate reportDate = LocalDate.of(2024, 2, 19); // Example Monday
        LocalDate startOfWeek = reportDate.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDateTime = endOfWeek.atTime(23, 59, 59);

        when(hearingRecordingSegmentRepository
                 .findByCreatedOnBetweenDatesWithHearingRecording(startOfWeekDateTime, endOfWeekDateTime))
            .thenThrow(new RuntimeException("Dummy error"));

        try {
            hearingReportService.createWeeklyReport(reportDate);
        } catch (RuntimeException exception) {
            assertEquals("Dummy error", exception.getMessage());
        }
        verify(hearingRecordingSegmentRepository, times(1))
            .findByCreatedOnBetweenDatesWithHearingRecording(startOfWeekDateTime, endOfWeekDateTime);
    }
}
