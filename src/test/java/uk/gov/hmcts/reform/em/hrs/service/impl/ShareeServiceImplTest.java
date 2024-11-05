package uk.gov.hmcts.reform.em.hrs.service.impl;

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
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.hearingRecordingWithNoDataBuilder;

@ExtendWith(MockitoExtension.class)
class ShareeServiceImplTest {
    @Mock
    private ShareesRepository shareesRepository;

    @InjectMocks
    private ShareeServiceImpl underTest;

    @Test
    void testShouldSaveEntity() {
        underTest.createAndSaveEntry(SHAREE_EMAIL_ADDRESS, hearingRecordingWithNoDataBuilder());

        verify(shareesRepository, times(1)).save(any(HearingRecordingSharee.class));
    }
}
