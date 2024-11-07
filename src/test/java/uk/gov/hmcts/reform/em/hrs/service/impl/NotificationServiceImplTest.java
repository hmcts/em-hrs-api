package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_DATE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_SEGMENT_DOWNLOAD_URLS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_TIMEOFDAY;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.convertObjectToJsonString;

class NotificationServiceImplTest {
    private static final String EMAIL_TEMPLATE_ID = "1e10b560-4a3f-49a7-81f7-c3c6eceab455";
    private final NotificationClientApi notificationClient = mock(NotificationClientApi.class);

    private final NotificationServiceImpl underTest = new NotificationServiceImpl(
        EMAIL_TEMPLATE_ID,
        notificationClient
    );

    @Test
    void sendEmailNotificationSuccessfully() throws Exception {
        final Map<String, Object> personalisation = makePersonalisation();
        final SendEmailResponse sendEmailResponse = makeSendEmailResponse();

        doReturn(sendEmailResponse)
            .when(notificationClient)
            .sendEmail(anyString(),
                       eq(SHAREE_EMAIL_ADDRESS),
                       eq(personalisation),
                       anyString());

        underTest.sendEmailNotification(
            CASE_REFERENCE, RECORDING_SEGMENT_DOWNLOAD_URLS,
            RECORDING_DATE, RECORDING_TIMEOFDAY,
            SHAREE_ID, SHAREE_EMAIL_ADDRESS
        );

        verify(notificationClient, times(1))
            .sendEmail(anyString(),
                       eq(SHAREE_EMAIL_ADDRESS),
                       eq(personalisation),
                       anyString());
    }

    @Test
    void sendEmailNotificationFailure() throws Exception {
        final Map<String, Object> personalisation = makePersonalisation();

        doThrow(NotificationClientException.class)
            .when(notificationClient)
            .sendEmail(anyString(),
                       eq(SHAREE_EMAIL_ADDRESS),
                       eq(personalisation),
                       anyString());

        assertThatExceptionOfType(NotificationClientException.class)
            .isThrownBy(
                () -> underTest.sendEmailNotification(CASE_REFERENCE, RECORDING_SEGMENT_DOWNLOAD_URLS,
                                                      RECORDING_DATE, RECORDING_TIMEOFDAY,
                                                      SHAREE_ID, SHAREE_EMAIL_ADDRESS)
            );
        verify(notificationClient, times(1))
            .sendEmail(anyString(), eq(SHAREE_EMAIL_ADDRESS), eq(personalisation), anyString());
    }

    private Map<String, Object> makePersonalisation() {
        final String pattern = "dd-MMM-yyyy";
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        final String formattedRecordingDateTime = dateTimeFormatter.format(RECORDING_DATE) + " " + RECORDING_TIMEOFDAY;

        return Map.of("case_reference", CASE_REFERENCE,
                      "hearing_recording_datetime", formattedRecordingDateTime,
                      "hearing_recording_segment_urls", RECORDING_SEGMENT_DOWNLOAD_URLS
        );
    }

    private SendEmailResponse makeSendEmailResponse() throws Exception {
        final String emailResponseString = convertObjectToJsonString(
            Map.of("id", UUID.randomUUID(),
                   "reference", "email-reference",
                   "content", Map.of("body", "email-body",
                                     "from_email", "fromEmail@email.com",
                                     "subject", "Test Email"
                ),
                   "template", Map.of("id", UUID.randomUUID(),
                                      "version", "0",
                                      "uri", "template-uri"
                )
            )
        );
        return new SendEmailResponse(emailResponseString);
    }
}
