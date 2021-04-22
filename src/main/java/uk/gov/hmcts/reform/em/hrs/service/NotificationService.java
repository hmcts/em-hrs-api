package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void sendEmailNotification(String caseReference,
                               LocalDateTime recordingDatetime,
                               List<String> recordingSegmentDownloadUrls,
                               UUID shareeId,
                               String shareeEmailAddress) throws NotificationClientException;
}
