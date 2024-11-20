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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonthlyAuditReportTaskTest {

    @Test
    void should_call_audit_report_service_and_send_email() throws IOException {
        // given
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var auditReportService = mock(MonthlyAuditReportTask.AuditReportService.class);

        File mockReport = mock(File.class);
        var now = LocalDate.now().minusMonths(1);
        when(auditReportService.createMonthlyReport(now.getMonth(), now.getYear())).thenReturn(mockReport);

        var task = new MonthlyAuditReportTask(hearingReportEmailService, auditReportService, new ArrayList<>());

        // when
        task.run();

        // then
        verify(auditReportService, times(1)).createMonthlyReport(now.getMonth(), now.getYear());
        verify(hearingReportEmailService, times(1)).sendReport(now, mockReport);
    }

    @Test
    void should_call_audit_report_service_for_each_report_start_date() throws IOException {
        // given
        var reportStartDateList = List.of(
            LocalDate.now(),
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusMonths(2)
        );
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var auditReportService = mock(MonthlyAuditReportTask.AuditReportService.class);

        File mockReport = mock(File.class);
        when(auditReportService.createMonthlyReport(any(Month.class), anyInt())).thenReturn(mockReport);

        var task = new MonthlyAuditReportTask(hearingReportEmailService, auditReportService, reportStartDateList);

        // when
        task.run();

        // then
        verify(auditReportService, times(3)).createMonthlyReport(any(Month.class), anyInt());
        verify(hearingReportEmailService, times(3)).sendReport(any(), any());
    }

    @Test
    void should_continue_processing_even_when_an_exception_occurs() throws IOException {
        // given
        var reportStartDateList = List.of(
            LocalDate.now(),
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusMonths(2)
        );
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var auditReportService = mock(MonthlyAuditReportTask.AuditReportService.class);

        File mockReport = mock(File.class);
        when(auditReportService.createMonthlyReport(any(Month.class), anyInt()))
            .thenThrow(new RuntimeException("Error for first date"))
            .thenReturn(mockReport)
            .thenReturn(mockReport);

        var task = new MonthlyAuditReportTask(hearingReportEmailService, auditReportService, reportStartDateList);

        // when
        task.run();

        // then
        verify(auditReportService, times(3)).createMonthlyReport(any(Month.class), anyInt());
        verify(hearingReportEmailService, times(2)).sendReport(any(), any());
    }

    @Test
    void should_add_previous_month_if_report_start_date_list_is_empty() throws IOException {
        // given
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var auditReportService = mock(MonthlyAuditReportTask.AuditReportService.class);

        File mockReport = mock(File.class);
        var previousMonth = LocalDate.now().minusMonths(1);
        when(auditReportService.createMonthlyReport(previousMonth.getMonth(), previousMonth.getYear()))
            .thenReturn(mockReport);

        var task = new MonthlyAuditReportTask(hearingReportEmailService, auditReportService, new ArrayList<>());

        // when
        task.run();

        // then
        verify(auditReportService, times(1))
            .createMonthlyReport(previousMonth.getMonth(), previousMonth.getYear());
        verify(hearingReportEmailService, times(1))
            .sendReport(previousMonth, mockReport);
    }
}
