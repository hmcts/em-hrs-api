package uk.gov.hmcts.reform.em.hrs.storage;

import java.util.Set;

public interface HearingRecordingStorage {
    Set<String> findByFolderName(String folderName);

    void copyRecording(String sourceUri, String filename);

    StorageReport getStorageReport();
}
