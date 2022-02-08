package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.exception.EmailNotificationException;
import uk.gov.hmcts.reform.em.hrs.exception.EmailRecipientNotFoundException;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SummaryReportServiceTest {

    @Mock
    private EmailSender emailSender;
    private String[] recipients = {"m@y.com", "q@z.com"};
    @Mock
    private HearingRecordingStorage hearingRecordingStorage;
    private SummaryReportService summaryReportService;

    @BeforeEach
    void setup() {
        summaryReportService = new SummaryReportService(emailSender, recipients, hearingRecordingStorage);
    }

    @Test
    void should_process() throws SendEmailException {
        given(hearingRecordingStorage.getStorageReport())
            .willReturn(new StorageReport(23L, 67L));
        summaryReportService.sendReport();
        verify(hearingRecordingStorage).getStorageReport();
        verify(emailSender).sendMessageWithAttachments(anyString(),anyString(),anyString(),any(),any());
    }

    @Test
    void should_omit_report_exceptions() {
        given(hearingRecordingStorage.getStorageReport())
            .willThrow(new ArithmeticException("Can not calculate"));
        summaryReportService.sendReport();
        verify(hearingRecordingStorage).getStorageReport();
        verifyNoInteractions(emailSender);
    }

    @Test
    void should_omit_email_exceptions() throws SendEmailException {
        given(hearingRecordingStorage.getStorageReport())
            .willReturn(mock(StorageReport.class));
        doThrow(new EmailNotificationException(new Exception("Email error")))
            .when(emailSender).sendMessageWithAttachments(anyString(),anyString(),anyString(),any(),any());;
        summaryReportService.sendReport();
        verify(hearingRecordingStorage).getStorageReport();
        verify(emailSender).sendMessageWithAttachments(anyString(),anyString(),anyString(),any(),any());
    }

    @Test
    void should_throw_if_empty_recipients() {
        assertThrows(
            EmailRecipientNotFoundException.class,
            () -> new SummaryReportService(null, new String[]{}, null)
        );
    }
}
