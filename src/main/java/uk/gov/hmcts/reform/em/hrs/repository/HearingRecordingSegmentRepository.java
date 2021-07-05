package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.util.List;
import java.util.UUID;


@Repository
public interface HearingRecordingSegmentRepository extends PagingAndSortingRepository<HearingRecordingSegment, UUID> {

    List<HearingRecordingSegment> findByHearingRecordingId(UUID hearingRecordingId);

    HearingRecordingSegment findByHearingRecordingIdAndRecordingSegment(UUID recordingId, Integer segment);

    HearingRecordingSegment findByFilename(String filename);
}
