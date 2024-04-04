package uk.gov.hmcts.reform.em.hrs.storage;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;

import java.util.Set;

public interface HearingRecordingStorage {
    Set<String> findByFolderName(String folderName);

    void copyRecording(HearingRecordingDto hrDto);

    StorageReport getStorageReport();

    HearingRecordingStorageImpl.BlobDetail findBlob(final HearingSource hearingSource, final String blobName);

    void listVHBlobs();
}
