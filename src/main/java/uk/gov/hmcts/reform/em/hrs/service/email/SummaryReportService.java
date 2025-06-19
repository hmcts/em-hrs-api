package uk.gov.hmcts.reform.em.hrs.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.util.Arrays;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyMap;

@Service
@Lazy
public class SummaryReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryReportService.class);

    private static final String SUBJECT_PREFIX = "Summary-Report-";

    private final EmailSender emailSender;

    private final String[] recipients;

    private final HearingRecordingStorage hearingRecordingStorage;
    private final String from;

    public SummaryReportService(
        EmailSender emailSender,
        @Value("${report.recipients}") String[] recipients,
        HearingRecordingStorage hearingRecordingStorage,
        @Value("${report.from}") String from
    ) {
        this.emailSender = emailSender;
        if (recipients == null || recipients.length == 0) {
            throw new EmailRecipientNotFoundException("No recipients configured for reports");
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }
        this.hearingRecordingStorage = hearingRecordingStorage;
        this.from = from;
    }

    public void sendReport() {
        try {
            var report = hearingRecordingStorage.getStorageReport();
            LOGGER.info("Report recipients: {}", this.recipients[0]);

            emailSender.sendMessageWithAttachments(
                SUBJECT_PREFIX + now(),
                createBody(report),
                from,
                recipients,
                emptyMap()
            );
        } catch (Exception ex) {
            LOGGER.error("Report sending failed ", ex);
        }
    }


    private String createBody(StorageReport report) {

        return "<html><body><h1>Blobstores Inspected</h1><h5>TOTAL</h5> CVP Count = "
            + report.cvpItemCount
            + " vs HRS CVP Count = "
            + report.hrsCvpItemCount
            + "<br><br><br>"
            + "<h5>TODAY "
            + report.today
            + "</h5> CVP Count = "
            + report.cvpItemCountToday
            + " vs HRS CVP Count = "
            + report.hrsCvpItemCountToday
            + "<br><br></body></html>";
    }
}
