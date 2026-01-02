package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


public class HearingReportEmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingReportEmailService.class);

    private final String subjectPrefix;

    private final EmailSender emailSender;

    private final String[] recipients;

    private final String from;

    private final Function<LocalDate,String> getReportAttachmentName;
    private final Optional<Function<LocalDate,String>> createEmailBodyOpt;


    public HearingReportEmailService(
        EmailSender emailSender,
        String[] recipients,
        String from,
        String subjectPrefix,
        Function<LocalDate, String> getReportAttachmentName,
        Optional<Function<LocalDate, String>> createEmailBodyOpt
    ) {
        this.emailSender = emailSender;
        if (ArrayUtils.isEmpty(recipients)) {
            throw new EmailRecipientNotFoundException("No recipients configured for reports");
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }
        this.from = from;
        this.subjectPrefix = subjectPrefix;
        this.getReportAttachmentName = getReportAttachmentName;
        this.createEmailBodyOpt = createEmailBodyOpt;
    }

    public HearingReportEmailService(
        EmailSender emailSender,
        String[] recipients,
        String from,
        String subjectPrefix,
        Function<LocalDate, String> getReportAttachmentName
    ) {
        this(emailSender, recipients, from, subjectPrefix, getReportAttachmentName, Optional.empty());
    }

    public void sendReport(LocalDate reportDate, File reportFile) {
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Report recipients: {}", Arrays.toString(this.recipients));
            }

            emailSender.sendMessageWithAttachments(
                this.subjectPrefix + reportDate,
                generateEmailBody(reportDate),
                from,
                recipients,
                Map.of(getReportAttachmentName.apply(reportDate), reportFile)
            );
        } catch (Exception ex) {
            LOGGER.error("Report sending failed ", ex);
        }
    }


    public String generateEmailBody(LocalDate date) {
        return createEmailBodyOpt
            .map(func -> func.apply(date))
            .orElseGet(() -> createDefaultEmailBody(date));
    }

    private String createDefaultEmailBody(LocalDate date) {
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
