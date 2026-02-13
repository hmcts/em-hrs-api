package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String TEMPLATE_ID = "1e10b560-4a3f-49a7-81f7-c3c6eceab455";

    @Mock
    private NotificationClientApi notificationClient;

    @Captor
    private ArgumentCaptor<Map<String, Object>> personalisationCaptor;

    @Captor
    private ArgumentCaptor<String> referenceCaptor;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(TEMPLATE_ID, notificationClient);
    }

    @Test
    void sendEmailNotificationShouldSendEmailSuccessfullyWithCorrectPersonalisation() throws Exception {
        // Given
        String caseReference = "case-ref-123";
        LocalDate recordingDate = LocalDate.of(2024, 3, 15);
        String timeOfDay = "AM";
        UUID shareeId = UUID.randomUUID();
        String shareeEmail = "test@example.com";
        List<String> downloadUrls = List.of("http://url1.com", "http://url2.com");

        // When
        notificationService.sendEmailNotification(
            caseReference,
            downloadUrls,
            recordingDate,
            timeOfDay,
            shareeId,
            shareeEmail
        );

        // Then
        verify(notificationClient).sendEmail(
            eq(TEMPLATE_ID),
            eq(shareeEmail),
            personalisationCaptor.capture(),
            referenceCaptor.capture()
        );

        Map<String, Object> personalisation = personalisationCaptor.getValue();
        assertThat(personalisation)
            .containsEntry("case_reference", caseReference)
            .containsEntry("hearing_recording_datetime", "15-Mar-2024 AM")
            .containsEntry("hearing_recording_segment_urls", downloadUrls);

        assertThat(referenceCaptor.getValue()).isEqualTo("hrs-grant-" + shareeId);
    }

    @Test
    void sendEmailNotificationShouldHandleEmptyListsAndDifferentTimes() throws Exception {
        // This test covers the "Edge cases" you had in separate tests (Empty list, PM time)
        // combining them saves code while maintaining the same logical coverage.

        List<String> emptyUrls = Collections.emptyList();
        LocalDate recordingDate = LocalDate.of(2024, 3, 15);

        notificationService.sendEmailNotification(
            "case-ref",
            emptyUrls,
            recordingDate,
            "PM",
            UUID.randomUUID(),
            "test@example.com"
        );

        verify(notificationClient).sendEmail(anyString(), anyString(), personalisationCaptor.capture(), anyString());

        Map<String, Object> personalisation = personalisationCaptor.getValue();

        assertThat(personalisation)
            .containsEntry("hearing_recording_segment_urls", emptyUrls)
            .containsEntry("hearing_recording_datetime", "15-Mar-2024 PM");
    }

    @Test
    void sendEmailNotificationShouldPropagateNotificationClientException() throws Exception {
        String shareeEmail = "test@example.com";
        NotificationClientException expectedException = new NotificationClientException("Notification failed");

        doThrow(expectedException)
            .when(notificationClient)
            .sendEmail(anyString(), anyString(), anyMap(), anyString());

        assertThatExceptionOfType(NotificationClientException.class)
            .isThrownBy(() -> notificationService.sendEmailNotification(
                "case-ref",
                List.of("http://url.com"),
                LocalDate.now(),
                "AM",
                UUID.randomUUID(),
                shareeEmail
            ))
            .withMessage("Notification failed");
    }
}
