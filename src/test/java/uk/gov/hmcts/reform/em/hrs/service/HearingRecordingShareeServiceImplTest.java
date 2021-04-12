package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;

@ExtendWith(MockitoExtension.class)
class HearingRecordingShareeServiceImplTest {
    @Mock
    private ShareesRepository shareesRepository;

    @InjectMocks
    private HearingRecordingShareeServiceImpl underTest;

    @Test
    void testShouldSaveEntity() {
        underTest.createAndSaveEntry(SHAREE_EMAIL_ADDRESS, HEARING_RECORDING);

        verify(shareesRepository, times(1)).save(any(HearingRecordingSharee.class));
    }
}
