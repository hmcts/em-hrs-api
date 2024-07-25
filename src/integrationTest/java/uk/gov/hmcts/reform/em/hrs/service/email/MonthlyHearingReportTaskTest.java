package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {
    MonthlyHearingReportTask.class,
    HearingReportEmailService.class,
    DateListConverter.class
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class MonthlyHearingReportTaskTest {

    @MockBean
    private HearingReportEmailService hearingReportEmailService;

    @Autowired
    private MonthlyHearingReportTask monthlyHearingReportTask;


    @Test
    public void testRunSuccess() {
        doNothing().when(hearingReportEmailService).sendReport(any(LocalDate.class));

        monthlyHearingReportTask.run();

        verify(hearingReportEmailService, times(3)).sendReport(any(LocalDate.class));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 7, 1));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 8, 31));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 9, 15));
    }

    @Test
    public void testRunWithException() {
        doThrow(new RuntimeException("Testing Exception"))
            .when(hearingReportEmailService)
            .sendReport(any(LocalDate.class));

        monthlyHearingReportTask.run();

        verify(hearingReportEmailService, times(3)).sendReport(any(LocalDate.class));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 7, 1));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 8, 31));
        verify(hearingReportEmailService).sendReport(LocalDate.of(2024, 9, 15));
    }

}
