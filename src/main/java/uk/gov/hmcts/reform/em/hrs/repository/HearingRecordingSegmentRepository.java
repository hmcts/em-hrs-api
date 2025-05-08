package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Repository
public interface HearingRecordingSegmentRepository extends JpaRepository<HearingRecordingSegment, UUID> {

    Set<HearingRecordingSegment> findByHearingRecordingFolderName(String folderName);

    List<HearingRecordingSegment> findByHearingRecordingId(UUID hearingRecordingId);

    HearingRecordingSegment findByHearingRecordingIdAndRecordingSegment(UUID recordingId, Integer segment);

    HearingRecordingSegment findByHearingRecordingIdAndFilename(UUID recordingId, String filename);

    HearingRecordingSegment findByFilename(String filename);

    @Query("SELECT h FROM HearingRecordingSegment h JOIN FETCH h.hearingRecording "
        + " WHERE h.createdOn BETWEEN :startDate AND :endDate")
    List<HearingRecordingSegment> findByCreatedOnBetweenDatesWithHearingRecording(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
            SELECT new uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto(
            null, hrs.id, null, null, hrs.filename)
            FROM HearingRecordingSegment hrs
            WHERE hrs.hearingRecording.id = :hearingRecordingId
            """)
    List<HearingRecordingDeletionDto> findFilenamesByHearingRecordingId(UUID hearingRecordingId);

    @Modifying
    @Query("""
            DELETE FROM HearingRecordingSegment hrs
            WHERE hrs.hearingRecording.id = :hearingRecordingId
            """)
    void deleteByHearingRecordingId(@Param("hearingRecordingId") UUID hearingRecordingId);
}
