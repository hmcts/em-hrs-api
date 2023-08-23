package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BlobIndexMarker {
    private static final String PROCESSED = "processed";
    private final BlobContainerClient blobContainerClient;

    public BlobIndexMarker(
        final @Qualifier("VhBlobContainerClient") BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public boolean setProcessed(String blobName) {
        var blobClient = blobContainerClient.getBlobClient(blobName);
        var tags = blobClient.getTags();
        tags.put(PROCESSED, "true");
        blobClient.setTags(tags);
        return true;
    }
}
