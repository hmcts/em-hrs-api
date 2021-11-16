package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface JobInProgressRepository extends PagingAndSortingRepository<JobInProgress, UUID> {
    void deleteByCreatedOnLessThan(LocalDateTime dateTime);

    //TODO filename also contains the folder name - possibly this should be removed as a low value tech debt
    Set<JobInProgress> findByFolderNameAndFilename(String folderName, String fileName);
}
