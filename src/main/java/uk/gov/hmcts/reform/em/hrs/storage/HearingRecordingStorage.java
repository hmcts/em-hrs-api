package uk.gov.hmcts.reform.em.hrs.storage;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.util.Set;

public interface HearingRecordingStorage {
    Set<String> findByFolderName(String folderName);

    void copyRecording(HearingRecordingDto hrDto);

    StorageReport getStorageReport();
}
