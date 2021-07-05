package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void sendEmailNotification(String caseReference, List<String> recordingSegmentDownloadUrls,
                               LocalDate recordingDatetime, String timeOfDay,
                               UUID shareeId, String shareeEmailAddress) throws NotificationClientException;
}
