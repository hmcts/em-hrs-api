package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.specialized.BlockBlobClient;

import java.io.IOException;
import java.io.OutputStream;

public interface BlobstoreClient {

    BlobInfo fetchBlobInfo(String filename, String hearingSource);

    void downloadFile(String filename, BlobRange blobRange, final OutputStream outputStream, String hearingSource)
        throws IOException;

    BlockBlobClient getBlobClient(String filename, String hearingSource);
}

