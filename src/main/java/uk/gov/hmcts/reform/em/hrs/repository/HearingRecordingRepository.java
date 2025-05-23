package uk.gov.hmcts.reform.em.hrs.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;

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
}
