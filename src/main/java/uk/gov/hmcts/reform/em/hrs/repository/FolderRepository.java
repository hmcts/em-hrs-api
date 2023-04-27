package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends PagingAndSortingRepository<Folder, UUID> {
    Optional<Folder> findByName(String folderName);

    Optional<Folder> findByNameAndHearingSource(String folderName, String hearingSource);
}
