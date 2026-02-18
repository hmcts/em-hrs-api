package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShareeServiceImplTest {
    @Mock
    private ShareesRepository shareesRepository;

    @InjectMocks
    private ShareeServiceImpl underTest;

    @Test
    void testShouldSaveEntity() {
        String shareeEmailAddress = "sharee@example.com";
        HearingRecording hearingRecording = new HearingRecording();

        underTest.createAndSaveEntry(shareeEmailAddress, hearingRecording);

        verify(shareesRepository, times(1)).save(any(HearingRecordingSharee.class));
    }
}
