package uk.gov.hmcts.reform.em.hrs.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HearingRecordingRepository extends JpaRepository<HearingRecording, UUID> {

    Optional<HearingRecording> findByRecordingRefAndFolderName(String recordingReference, String folderName);

    Optional<HearingRecording> findByCcdCaseId(Long caseId);

    @Modifying
    @Transactional
    @Query("delete from HearingRecording s where s.createdOn < :#{#createddate} and s.ccdCaseId is null")
    void deleteStaleRecordsWithNullCcdCaseId(@Param("createddate") LocalDateTime createddate);

    List<HearingRecording> deleteByCcdCaseIdIn(Collection<Long> ccdCaseIds);

    @Query("SELECT hr.ccdCaseId FROM HearingRecording hr JOIN hr.segments hrs WHERE hrs.filename = :filename")
    Long findCcdCaseIdByFilename(@Param("filename") String filename);

    @Query("""
        SELECT new uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto(
        hr.id, null, null, hr.hearingSource, null)
        FROM HearingRecording hr WHERE hr.ccdCaseId IN :ccdCaseIds
        """)
    List<HearingRecordingDeletionDto> findHearingRecordingIdsAndSourceByCcdCaseIds(@Param("ccdCaseIds")
                                                                                   Collection<Long> ccdCaseIds);


    @Modifying
    @Query("""
            DELETE FROM HearingRecording hr
            WHERE hr.id IN :hearingRecordingIds
            """)
    void deleteByHearingRecordingIds(@Param("hearingRecordingIds") Collection<UUID> hearingRecordingIds);

}
