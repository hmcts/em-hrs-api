package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface JobInProgressRepository extends JpaRepository<JobInProgress, UUID> {

    @Modifying
    @Query("delete from JobInProgress s where s.createdOn < :#{#dateTime} or s.createdOn is null")
    void deleteByCreatedOnLessThan(@Param("dateTime") LocalDateTime dateTime);

    //TODO filename also contains the folder name - possibly this should be removed as a low value tech debt
    Set<JobInProgress> findByFolderNameAndFilename(String folderName, String fileName);
}
