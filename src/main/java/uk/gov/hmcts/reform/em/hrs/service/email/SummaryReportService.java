package uk.gov.hmcts.reform.em.hrs.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyMap;

@Service
public class SummaryReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryReportService.class);

    private static final String SUBJECT_PREFIX = "Summary-Report-";
    private static final String MAIL_FROM = "hrs-api@HMCTS.NET";

    private final EmailSender emailSender;

    private final String[] recipients;

    private final HearingRecordingStorage hearingRecordingStorage;

    public SummaryReportService(
        EmailSender emailSender,
        @Value("${report.recipients}") String[] recipients,
        HearingRecordingStorage hearingRecordingStorage
    ) {
        this.emailSender = emailSender;
        this.recipients = recipients;
        this.hearingRecordingStorage = hearingRecordingStorage;
    }

    public void sendReport() {
        try {
            var report = hearingRecordingStorage.getStorageReport();
            emailSender.sendMessageWithAttachments(
                SUBJECT_PREFIX + now(),
                createBody(report),
                MAIL_FROM,
                recipients,
                emptyMap()
            );
        } catch (Exception ex) {
            LOGGER.error("Report sending failed ", ex);
        }
    }


    private String createBody(StorageReport report) {
        String reportStr = "CVP Count = " + report.cvpItemCount;
        reportStr += " vs HRS Count = " + report.hrsItemCount;
        return "Blobstores Inspected<p>" + reportStr.replace("\n", "<p>");
    }
}
