package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import static java.time.LocalDateTime.now;

@Service
@Lazy
public class HearingReportEmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingReportEmailService.class);

    private static final String SUBJECT_PREFIX = "Monthly-hearing-report-";
    private static final String ATTACHMENT_PREFIX = "Monthly-hearing-report-";

    private final EmailSender emailSender;

    private final String[] recipients;

    private final HearingReportService hearingReportService;
    private final String from;

    public HearingReportEmailService(
        EmailSender emailSender,
        @Value("${report.monthly-hearing.recipients}") String[] recipients,
        HearingReportService hearingReportService,
        @Value("${report.from}") String from
    ) {
        this.emailSender = emailSender;
        if (ArrayUtils.isEmpty(recipients)) {
            throw new EmailRecipientNotFoundException("No recipients configured for reports");
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }
        this.hearingReportService = hearingReportService;
        this.from = from;
    }

    public void sendReport() {
        try {
            var reportDate = LocalDate.now().minusMonths(1);
            var reportFile = hearingReportService.createMonthlyReport(reportDate.getMonth(),reportDate.getYear());
            LOGGER.info("Report recipients: {}", this.recipients[0]);

            emailSender.sendMessageWithAttachments(
                SUBJECT_PREFIX + now(),
                createBody(reportDate),
                from,
                recipients,
                Map.of(getReportAttachmentName(reportDate), reportFile)
            );
        } catch (Exception ex) {
            LOGGER.error("Report sending failed ", ex);
        }
    }


    private String getReportAttachmentName(LocalDate reportDate) {
        return ATTACHMENT_PREFIX + reportDate.getMonth() + "-" + reportDate.getYear();
    }

    private String createBody(LocalDate date) {
        return """
            <html>
                <body>
                    <h1>Hearing Recording Report for %s/%d</h1>
                    <br>
                    <br><br>
                </body>
            </html>
            """.formatted(date.getMonth(), date.getYear());
    }
}
