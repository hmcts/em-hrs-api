package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.util.List;
import java.util.Set;
import java.util.UUID;


@Repository
public interface HearingRecordingSegmentRepository extends JpaRepository<HearingRecordingSegment, UUID> {

    Set<HearingRecordingSegment> findByHearingRecordingFolderName(String folderName);

    List<HearingRecordingSegment> findByHearingRecordingId(UUID hearingRecordingId);

    HearingRecordingSegment findByHearingRecordingIdAndRecordingSegment(UUID recordingId, Integer segment);

    HearingRecordingSegment findByFilename(String filename);


}
