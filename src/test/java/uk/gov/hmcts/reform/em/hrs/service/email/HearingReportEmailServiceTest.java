package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HearingReportEmailServiceTest {

    @Mock
    private EmailSender emailSender;

    private HearingReportEmailService hearingReportEmailService;

    private Function<LocalDate, String> func
        = HearingReportEmailServiceConfig.monthlyReportAttachmentName("Monthly-hearing-report-");

    @BeforeEach
    void setUp() {
        hearingReportEmailService = new HearingReportEmailService(
            emailSender,
            new String[]{"recipient@example.com"},
            "sender@example.com",
            "Monthly hearing report for ",
            func
        );
    }

    @Test
    void should_throw_exception_when_recipients_are_null() {
        assertThrows(EmailRecipientNotFoundException.class, () -> {
            new HearingReportEmailService(
                emailSender,
                null,
                "sender@example.com",
                "Monthly hearing report for ",
                func
            );
        });
    }

    @Test
    void should_throw_exception_when_recipients_are_empty() {
        assertThrows(EmailRecipientNotFoundException.class, () -> {
            new HearingReportEmailService(
                emailSender,
                new String[]{},
                "sender@example.com",
                "Monthly hearing report for ",
                func
            );
        });
    }

    @Test
    void should_send_report_email_with_attachment() throws Exception {

        LocalDate reportDate = LocalDate.now().minusMonths(1);

        File reportFile = new File("report.csv");

        hearingReportEmailService.sendReport(reportDate, reportFile);

        verify(emailSender, times(1)).sendMessageWithAttachments(
            contains("Monthly hearing report "),
            any(String.class),
            eq("sender@example.com"),
            eq(new String[]{"recipient@example.com"}),
            eq(Map.of(
                   "Monthly-hearing-report-" + reportDate.getMonth() + "-" + reportDate.getYear() + ".csv",
                   reportFile
               )
            )
        );
    }
}
