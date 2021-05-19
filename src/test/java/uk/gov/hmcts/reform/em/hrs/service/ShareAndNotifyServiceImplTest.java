package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CaseDataContentCreator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.*;

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
        doReturn(Optional.of(HEARING_RECORDING_WITH_SEGMENTS))
            .when(hearingRecordingRepository).findByCcdCaseId(CCD_CASE_ID);
        doReturn(HEARING_RECORDING_SHAREE)
            .when(shareeService).createAndSaveEntry(SHAREE_EMAIL_ADDRESS, HEARING_RECORDING_WITH_SEGMENTS);
        doNothing()
            .when(notificationService)
            .sendEmailNotification(CASE_REFERENCE,
                                   RECORDING_DATETIME,
                                   List.copyOf(Collections.singleton("/hearing-recordings/1234/segments/0")),
                                   SHAREE_ID,
                                   SHAREE_EMAIL_ADDRESS);
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS,
                         "recordingFiles", Collections.singletonList(CASE_RECORDING_FILE)))
            .id(CCD_CASE_ID)
            .build();
        doReturn(Collections.singletonList(CASE_RECORDING_FILE))
            .when(caseDataCreator).extractRecordingFiles(caseDetails.getData());

        underTest.shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);

        verify(hearingRecordingRepository, times(1)).findByCcdCaseId(CCD_CASE_ID);
        verify(shareeService, times(1))
            .createAndSaveEntry(SHAREE_EMAIL_ADDRESS, HEARING_RECORDING_WITH_SEGMENTS);
        verify(notificationService, times(1))
            .sendEmailNotification(CASE_REFERENCE,
                                   RECORDING_DATETIME,
                                   List.copyOf(
                                       Collections.singleton("https://xui.domain/hearing-recordings/1234/segments/0")
                                   ),
                                   SHAREE_ID,
                                   SHAREE_EMAIL_ADDRESS);
    }
}
