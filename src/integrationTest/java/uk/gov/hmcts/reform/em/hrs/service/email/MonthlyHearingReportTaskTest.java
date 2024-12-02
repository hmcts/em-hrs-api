package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {
    MonthlyHearingReportTask.class,
    HearingReportEmailService.class,
    DateListConverter.class
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class MonthlyHearingReportTaskTest {

    @MockitoBean
    private HearingReportEmailService hearingReportEmailService;

    @MockitoBean
    private HearingReportService hearingReportService;

    @Autowired
    private MonthlyHearingReportTask monthlyHearingReportTask;


    @Test
    public void testRunSuccess() throws IOException {
        doNothing().when(hearingReportEmailService).sendReport(any(LocalDate.class), any(File.class));

        File mockFile1 = mock(File.class);
        when(hearingReportService.createMonthlyReport(Month.JULY, 2024)).thenReturn(mockFile1);
        File mockFile2 = mock(File.class);
        when(hearingReportService.createMonthlyReport(Month.AUGUST, 2024)).thenReturn(mockFile2);
        File mockFile3 = mock(File.class);
        when(hearingReportService.createMonthlyReport(Month.SEPTEMBER, 2024)).thenReturn(mockFile3);

        // then
        monthlyHearingReportTask.run();

        verify(hearingReportEmailService, times(3)).sendReport(any(LocalDate.class), any(File.class));
        InOrder inOrder = inOrder(hearingReportEmailService);
        inOrder.verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 7, 1), mockFile1);
        inOrder.verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 8, 31), mockFile2);
        inOrder.verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 9, 15), mockFile3);
    }

    @Test
    public void testRunWithException() throws IOException {
        doThrow(new RuntimeException("Testing Exception"))
            .when(hearingReportEmailService)
            .sendReport(any(LocalDate.class), any(File.class));

        File mockFile1 = mock(File.class);
        when(hearingReportService.createMonthlyReport(Month.JULY, 2024)).thenReturn(mockFile1);
        when(hearingReportService.createMonthlyReport(Month.AUGUST, 2024)).thenReturn(mockFile1);
        when(hearingReportService.createMonthlyReport(Month.SEPTEMBER, 2024)).thenReturn(mockFile1);

        monthlyHearingReportTask.run();

        verify(hearingReportEmailService, times(3)).sendReport(any(LocalDate.class), any(File.class));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 7, 1), mockFile1);
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 8, 31), mockFile1);
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 9, 15), mockFile1);
    }

}
