package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;

public interface JobInProgressRepository extends PagingAndSortingRepository<JobInProgress, UUID> {

    @Modifying
    @Transactional
    @Query("delete from JobInProgress s where s.createdOn < :#{#dateTime} or s.createdOn is null")
    void deleteByCreatedOnLessThan(@Param("dateTime") LocalDateTime dateTime);

    //TODO filename also contains the folder name - possibly this should be removed as a low value tech debt
    Set<JobInProgress> findByFolderNameAndFilename(String folderName, String fileName);
}
