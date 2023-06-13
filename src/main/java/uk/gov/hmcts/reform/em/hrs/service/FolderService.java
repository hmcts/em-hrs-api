package uk.gov.hmcts.reform.em.hrs.service;

import jakarta.validation.constraints.NotNull;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;

import java.util.Set;

public interface FolderService {
    Set<String> getStoredFiles(String folderName);

    Folder getFolderByName(@NotNull String folderName);
}
