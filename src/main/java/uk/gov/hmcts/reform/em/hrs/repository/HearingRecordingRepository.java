package uk.gov.hmcts.reform.em.hrs.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HearingRecordingRepository extends PagingAndSortingRepository<HearingRecording, UUID> {

    @Query("select s from HearingRecording s where s.deleted = false and s.createdBy = :#{#creator}")
    Page<HearingRecording> findByCreatedBy(@Param("creator") String creator, @NonNull Pageable pageable);

    Optional<HearingRecording> findByRecordingRefAndFolderName(String recordingReference, String folderName);

    Optional<HearingRecording> findByCcdCaseId(Long caseId);

    Optional<HearingRecording> findByIdAndDeleted(UUID uuid, boolean deleted);

    List<HearingRecording> findByFolder(String folderName);

    @Modifying
    @Query("delete from HearingRecording s where s.createdOn < :#{#createddate} and s.ccdCaseId is null")
    void deleteStaleRecordsWithNullCcdCaseId(@Param("createddate") LocalDateTime createddate);
}
