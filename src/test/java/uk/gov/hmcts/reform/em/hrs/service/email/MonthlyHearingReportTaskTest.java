package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonthlyHearingReportTaskTest {


    @Test
    void should_call_summary_report_service() throws IOException {
        // given
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var hearingReportService = mock(HearingReportService.class);

        File mockReport = mock(File.class);
        var now = LocalDate.now().minusMonths(1);
        when(hearingReportService.createMonthlyReport(now.getMonth(),now.getYear())).thenReturn(mockReport);
        var task = new MonthlyHearingReportTask(hearingReportEmailService, hearingReportService, new ArrayList<>());

        // when
        task.run();

        // then
        verify(hearingReportEmailService, times(1))
            .sendReport(LocalDate.now().minusMonths(1), mockReport);
    }

    @Test
    void should_call_summary_report_service_as_many_time_as_reportStartDate_count() throws IOException {
        // given

        var reportStartDateList = List.of(
            LocalDate.now(),
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusMonths(2)
        );
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var hearingReportService = mock(HearingReportService.class);

        doThrow(new RuntimeException("testing dummy error"))
            .when(hearingReportEmailService).sendReport(any(LocalDate.class), any(File.class));

        File mockReport = mock(File.class);

        when(hearingReportService.createMonthlyReport(any(Month.class), anyInt())).thenReturn(mockReport);

        var task = new MonthlyHearingReportTask(hearingReportEmailService, hearingReportService, reportStartDateList);

        // when
        task.run();

        // then
        verify(hearingReportEmailService, times(3)).sendReport(any(), any());
        verify(hearingReportEmailService, times(1))
            .sendReport(LocalDate.now(), mockReport);
        verify(hearingReportEmailService, times(1))
            .sendReport(LocalDate.now().minusMonths(1), mockReport);
        verify(hearingReportEmailService, times(1))
            .sendReport(LocalDate.now().minusMonths(2), mockReport);
    }

}
