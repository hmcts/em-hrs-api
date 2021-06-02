package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CaseDataContentCreator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_RECORDING_FILE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_SHAREE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_DATE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_TIMEOFDAY;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_ID;

@ExtendWith({MockitoExtension.class})
class ShareAndNotifyServiceImplTest {

    private final ShareeService shareeService = mock(ShareeService.class);
    private final HearingRecordingRepository hearingRecordingRepository = mock(HearingRecordingRepository.class);
    private final CaseDataContentCreator caseDataCreator = mock(CaseDataContentCreator.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final ShareAndNotifyServiceImpl underTest = new ShareAndNotifyServiceImpl(
        hearingRecordingRepository,
        shareeService,
        notificationService,
        caseDataCreator,
        "https://xui.domain"
    );

    @Test
    void testShouldSendNotificationSuccessfully() throws Exception {
        CaseHearingRecording caseData = CaseHearingRecording.builder()
            .shareeEmail(SHAREE_EMAIL_ADDRESS)
            .recordingReference(CASE_REFERENCE)
            .recordingDate(RECORDING_DATE)
            .recordingTimeOfDay(RECORDING_TIMEOFDAY)
            .recordingFiles(Collections.singletonList(Map.of("value", CASE_RECORDING_FILE))).build();
        doReturn(caseData).when(caseDataCreator).getCaseRecordingObject(Map.of("case", "data"));
        doReturn(List.of(
            CaseDocument.builder().binaryUrl("http://em-hrs-api.com/hearing-recordings/1234/segments/0").build()
        )).when(caseDataCreator).extractCaseDocuments(caseData);
        doReturn(Optional.of(HEARING_RECORDING_WITH_SEGMENTS))
            .when(hearingRecordingRepository).findByCcdCaseId(CCD_CASE_ID);
        doReturn(HEARING_RECORDING_SHAREE)
            .when(shareeService).createAndSaveEntry(SHAREE_EMAIL_ADDRESS, HEARING_RECORDING_WITH_SEGMENTS);
        doNothing()
            .when(notificationService)
            .sendEmailNotification(
                CASE_REFERENCE, List.copyOf(Collections.singleton("/hearing-recordings/1234/segments/0")),
                RECORDING_DATE, RECORDING_TIMEOFDAY,
                SHAREE_ID, SHAREE_EMAIL_ADDRESS
            );

        underTest.shareAndNotify(CCD_CASE_ID, Map.of("case", "data"), AUTHORIZATION_TOKEN);

        verify(hearingRecordingRepository, times(1)).findByCcdCaseId(CCD_CASE_ID);
        verify(shareeService, times(1))
            .createAndSaveEntry(SHAREE_EMAIL_ADDRESS, HEARING_RECORDING_WITH_SEGMENTS);
        verify(notificationService, times(1))
            .sendEmailNotification(
                CASE_REFERENCE,
                List.copyOf(Collections.singleton("https://xui.domain/hearing-recordings/1234/segments/0")),
                RECORDING_DATE, RECORDING_TIMEOFDAY,
                SHAREE_ID, SHAREE_EMAIL_ADDRESS
            );
    }
}
