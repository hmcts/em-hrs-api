package uk.gov.hmcts.reform.em.hrs.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface JobInProgressRepository extends CrudRepository<JobInProgress, UUID> {

    @Modifying
    @Transactional
    @Query("delete from JobInProgress s where s.createdOn < :#{#dateTime} or s.createdOn is null")
    void deleteByCreatedOnLessThan(@Param("dateTime") LocalDateTime dateTime);

    Set<JobInProgress> findByFolderNameAndFilename(String folderName, String fileName);
}
