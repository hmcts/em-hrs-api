package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RANDOM_UUID;

@ExtendWith(MockitoExtension.class)
class SegmentServiceImplTest {
    @Mock
    private HearingRecordingSegmentRepository segmentRepository;

    @InjectMocks
    private SegmentServiceImpl underTest;

    @Test
    void testFindByRecordingId() {
        doReturn(Collections.emptyList()).when(segmentRepository).findByHearingRecordingId(RANDOM_UUID);

        final List<HearingRecordingSegment> segments = underTest.findByRecordingId(RANDOM_UUID);

        assertThat(segments).isEmpty();
        verify(segmentRepository, times(1)).findByHearingRecordingId(RANDOM_UUID);
    }

}
