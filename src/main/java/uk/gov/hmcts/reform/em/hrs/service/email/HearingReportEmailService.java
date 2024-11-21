package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;


public class HearingReportEmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingReportEmailService.class);

    private final String subjectPrefix;
    private final String attachmentPrefix;

    private final EmailSender emailSender;

    private final String[] recipients;

    private final String from;

    public HearingReportEmailService(
        EmailSender emailSender,
        String[] recipients,
        String from,
        String subjectPrefix,
        String attachmentPrefix
    ) {
        this.emailSender = emailSender;
        if (ArrayUtils.isEmpty(recipients)) {
            throw new EmailRecipientNotFoundException("No recipients configured for reports");
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }
        this.from = from;
        this.subjectPrefix = subjectPrefix;
        this.attachmentPrefix = attachmentPrefix;
    }

    public void sendReport(LocalDate reportDate, File reportFile) {
        try {
            LOGGER.info("Report recipients: {}", this.recipients[0]);

            emailSender.sendMessageWithAttachments(
                this.subjectPrefix + reportDate,
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
        return this.attachmentPrefix + reportDate.getMonth() + "-" + reportDate.getYear() + ".csv";
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
