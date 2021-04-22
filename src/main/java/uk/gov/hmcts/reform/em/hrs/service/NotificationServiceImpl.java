package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final String templateId;
    private final NotificationClientApi notificationClient;

    @Autowired
    public NotificationServiceImpl(@Value("${notify.email.template}") final String templateId,
                                   final NotificationClientApi notificationClient) {
        this.templateId = templateId;
        this.notificationClient = notificationClient;
    }

    @Override
    public void sendEmailNotification(final String caseReference,
                                      final LocalDateTime recordingDatetime,
                                      final List<String> recordingSegmentDownloadUrls,
                                      final UUID shareeId,
                                      final String shareeEmailAddress) throws NotificationClientException {
        notificationClient.sendEmail(
            templateId,
            shareeEmailAddress,
            createPersonalisation(caseReference, recordingDatetime, recordingSegmentDownloadUrls),
            String.format("hrs-grant-%s", shareeId)
        );
    }

    private Map<String, Object> createPersonalisation(final String caseReference,
                                                      final LocalDateTime recordingDatetime,
                                                      final List<String> recordingSegmentDownloadUrls) {
        final String pattern = "DD-MMM-YYYY HH-MM";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        final String formattedRecordingDateTime = dateTimeFormatter.format(recordingDatetime);

        return Map.of("case_reference", caseReference,
                      "hearing_recording_datetime", formattedRecordingDateTime,
                      "hearing_recording_segment_urls", recordingSegmentDownloadUrls
        );
    }
}
