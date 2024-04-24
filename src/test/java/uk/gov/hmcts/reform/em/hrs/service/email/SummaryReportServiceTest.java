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

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
        summaryReportService =
            new SummaryReportService(emailSender, recipients, hearingRecordingStorage, "d@e.com");
    }

    @Test
    void should_process() throws SendEmailException {
        var today = LocalDate.now();
        given(hearingRecordingStorage.getStorageReport()).willReturn(new StorageReport(
            today,
            new StorageReport.HrsSourceVsDestinationCounts(23L, 67L, 55L, 45L),
            new StorageReport.HrsSourceVsDestinationCounts(1123L, 1107L, 114L, 105L)
        ));
        summaryReportService.sendReport();
        verify(hearingRecordingStorage).getStorageReport();
        String[] recipients = {"m@y.com", "q@z.com"};
        String titleContains = "Summary-Report-" + LocalDate.now();
        verify(emailSender)
            .sendMessageWithAttachments(
                contains(titleContains),
                eq("<html><body><h1>Blobstores Inspected</h1><h5>TOTAL</h5> "
                       + "CVP Count = 23 vs HRS CVP Count = 67<br><br>VH Count = 1123 vs HRS VH Count = 1107"
                       + "<br><br><br><h5>TODAY " + today
                       + "</h5> CVP Count = 55 vs HRS CVP Count = 45<br><br>"
                       + "VH Count = 114 vs HRS Count = 105<br><br><br></body></html>"),
                eq("d@e.com"),
                eq(recipients),
                eq(Map.of())
            );
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
            () -> new SummaryReportService(null, new String[]{}, null, "from@g.com")
        );
    }
}
