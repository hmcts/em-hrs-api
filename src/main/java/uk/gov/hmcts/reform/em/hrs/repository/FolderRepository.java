package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends CrudRepository<Folder, UUID> {
    Optional<Folder> findByName(String folderName);
}
