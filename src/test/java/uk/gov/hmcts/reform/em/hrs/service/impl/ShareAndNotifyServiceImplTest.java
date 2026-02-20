package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.exception.GovNotifyErrorException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.Constants;
import uk.gov.hmcts.reform.em.hrs.service.NotificationService;
import uk.gov.hmcts.reform.em.hrs.service.ShareeService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CaseDataContentCreator;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareAndNotifyServiceImplTest {

    private static final String XUI_DOMAIN = "https://xui.com";
    private static final Long CASE_ID = 1234567890L;
    private static final String AUTHORISATION_TOKEN = "Bearer token";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String INVALID_EMAIL = "invalid-email";
    private static final String BINARY_URL = "http://dm-store/hearing-recordings/doc-123";

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
    @Mock
    private ShareeService shareeService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CaseDataContentCreator caseDataCreator;
    @Mock
    private AuditEntryService auditEntryService;

    @Captor
    private ArgumentCaptor<List<String>> urlCaptor;

    private ShareAndNotifyServiceImpl shareAndNotifyService;
    private Map<String, Object> caseDataMap;

    @BeforeEach
    void setUp() {
        shareAndNotifyService = new ShareAndNotifyServiceImpl(
            hearingRecordingRepository,
            shareeService,
            notificationService,
            caseDataCreator,
            XUI_DOMAIN,
            auditEntryService
        );
        caseDataMap = Map.of("key", "value");
    }

    @Test
    void shareAndNotifyShouldThrowValidationErrorWhenEmailIsInvalid() {
        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setShareeEmail(INVALID_EMAIL);

        when(caseDataCreator.getCaseRecordingObject(caseDataMap)).thenReturn(caseHearingRecording);

        assertThatThrownBy(() -> shareAndNotifyService.shareAndNotify(CASE_ID, caseDataMap, AUTHORISATION_TOKEN))
            .isInstanceOf(ValidationErrorException.class);

        verify(auditEntryService).logOnly(CASE_ID, AuditActions.SHARE_GRANT_FAIL);
        verify(hearingRecordingRepository, never()).findByCcdCaseId(anyLong());
    }

    @Test
    void shareAndNotifyShouldThrowHearingRecordingNotFoundExceptionWhenRecordingDoesNotExist() {
        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setShareeEmail(VALID_EMAIL);

        when(caseDataCreator.getCaseRecordingObject(caseDataMap)).thenReturn(caseHearingRecording);
        when(hearingRecordingRepository.findByCcdCaseId(CASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareAndNotifyService.shareAndNotify(CASE_ID, caseDataMap, AUTHORISATION_TOKEN))
            .isInstanceOf(HearingRecordingNotFoundException.class);
    }

    @Test
    void shareAndNotifyShouldSuccessfullyShareAndNotifyUser() throws NotificationClientException {
        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setShareeEmail(VALID_EMAIL);
        caseHearingRecording.setRecordingDate(LocalDate.now());
        caseHearingRecording.setRecordingTimeOfDay("AM");

        CaseDocument caseDocument = new CaseDocument();
        caseDocument.setBinaryUrl(BINARY_URL);

        HearingRecording recording = new HearingRecording();
        recording.setCaseRef("CASE-REF");

        HearingRecordingSharee sharee = new HearingRecordingSharee();
        sharee.setId(UUID.randomUUID());

        when(caseDataCreator.getCaseRecordingObject(caseDataMap)).thenReturn(caseHearingRecording);
        when(hearingRecordingRepository.findByCcdCaseId(CASE_ID)).thenReturn(Optional.of(recording));
        when(shareeService.createAndSaveEntry(VALID_EMAIL, recording)).thenReturn(sharee);
        when(caseDataCreator.extractCaseDocuments(caseHearingRecording)).thenReturn(List.of(caseDocument));

        shareAndNotifyService.shareAndNotify(CASE_ID, caseDataMap, AUTHORISATION_TOKEN);

        verify(shareeService).createAndSaveEntry(VALID_EMAIL, recording);
        verify(auditEntryService).createAndSaveEntry(sharee, AuditActions.SHARE_GRANT_OK);

        verify(notificationService).sendEmailNotification(
            eq("CASE-REF"),
            urlCaptor.capture(),
            eq(caseHearingRecording.getRecordingDate()),
            eq("AM"),
            eq(sharee.getId()),
            eq(VALID_EMAIL)
        );

        List<String> capturedUrls = urlCaptor.getValue();
        assertThat(capturedUrls).hasSize(1);
        String expectedUrl = XUI_DOMAIN + "/hearing-recordings/doc-123" + Constants.SHAREE;
        assertThat(capturedUrls.get(0)).isEqualTo(expectedUrl);

        verify(auditEntryService).logOnly(CASE_ID, AuditActions.NOTIFY_OK);
    }

    @Test
    void shareAndNotifyShouldThrowGovNotifyErrorExceptionWhenNotificationFails() throws NotificationClientException {
        CaseHearingRecording caseHearingRecording = new CaseHearingRecording();
        caseHearingRecording.setShareeEmail(VALID_EMAIL);

        HearingRecording recording = new HearingRecording();
        HearingRecordingSharee sharee = new HearingRecordingSharee();

        when(caseDataCreator.getCaseRecordingObject(caseDataMap)).thenReturn(caseHearingRecording);
        when(hearingRecordingRepository.findByCcdCaseId(CASE_ID)).thenReturn(Optional.of(recording));
        when(shareeService.createAndSaveEntry(VALID_EMAIL, recording)).thenReturn(sharee);
        when(caseDataCreator.extractCaseDocuments(caseHearingRecording)).thenReturn(Collections.emptyList());

        doThrow(new NotificationClientException("Notify Error"))
            .when(notificationService)
            .sendEmailNotification(any(), anyList(), any(), any(), any(), any());

        assertThatThrownBy(() -> shareAndNotifyService.shareAndNotify(CASE_ID, caseDataMap, AUTHORISATION_TOKEN))
            .isInstanceOf(GovNotifyErrorException.class)
            .hasCauseInstanceOf(NotificationClientException.class);

        verify(auditEntryService).logOnly(CASE_ID, AuditActions.NOTIFY_FAIL);
    }
}
