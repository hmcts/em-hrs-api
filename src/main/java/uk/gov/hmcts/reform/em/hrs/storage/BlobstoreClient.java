package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.models.BlobProperties;

import java.io.OutputStream;

public interface BlobstoreClient {

    void downloadFile(String filename, OutputStream output);

    BlobProperties getBlobProperties(final String filename);
}
