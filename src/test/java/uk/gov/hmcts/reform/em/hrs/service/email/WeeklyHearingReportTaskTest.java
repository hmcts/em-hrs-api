package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyHearingReportTaskTest {

    @Mock
    private HearingReportEmailService hearingReportEmailService;

    @Mock
    private HearingReportService hearingReportService;


    @Test
    void should_call_summary_report_service() throws IOException {
        File mockReport = mock(File.class);
        LocalDate now = LocalDate.now().minusWeeks(1);

        when(hearingReportService.createWeeklyReport(any(LocalDate.class))).thenReturn(mockReport);
        when(hearingReportService.getStartOfWeekDateTime(any(LocalDate.class))).thenReturn(now);

        WeeklyHearingReportTask task = new WeeklyHearingReportTask(
            hearingReportEmailService,
            hearingReportService,
            new ArrayList<>()
        );
        task.run();

        verify(hearingReportEmailService, times(1)).sendReport(now, mockReport);
    }

    @Test
    void should_call_summary_report_service_as_many_times_as_reportStartDate_count() throws IOException {
        List<LocalDate> reportStartDateList = List.of(
            LocalDate.now().minusWeeks(1),
            LocalDate.now().minusWeeks(2),
            LocalDate.now().minusWeeks(3)
        );

        File mockReport = mock(File.class);
        when(hearingReportService.createWeeklyReport(any(LocalDate.class))).thenReturn(mockReport);

        WeeklyHearingReportTask task = new WeeklyHearingReportTask(
            hearingReportEmailService,
            hearingReportService,
            reportStartDateList
        );
        task.run();

        verify(hearingReportEmailService, times(3)).sendReport(any(), any());
        for (LocalDate date : reportStartDateList) {
            verify(hearingReportEmailService, times(1)).sendReport(date, mockReport);
        }
    }

    @Test
    void should_handle_exception_during_report_sending() throws IOException {
        LocalDate now = LocalDate.now().minusWeeks(1);
        File mockReport = mock(File.class);
        when(hearingReportService.createWeeklyReport(now)).thenReturn(mockReport);
        doThrow(new RuntimeException("testing dummy error")).when(hearingReportEmailService).sendReport(
            any(LocalDate.class),
            any(File.class)
        );

        WeeklyHearingReportTask task = new WeeklyHearingReportTask(
            hearingReportEmailService,
            hearingReportService,
            List.of(now)
        );
        task.run();

        verify(hearingReportEmailService, times(1)).sendReport(now, mockReport);
    }
}
