package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BlobIndexMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobIndexMarker.class);

    private static final String PROCESSED = "processed";
    private final BlobContainerClient blobContainerClient;

    public BlobIndexMarker(
        final @Qualifier("vhBlobContainerClient") BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public boolean setProcessed(String blobName) {
        var blobClient = blobContainerClient.getBlobClient(blobName);
        var tags = blobClient.getTags();
        LOGGER.info("blobName {} existing tags {}", blobName, tags);
        tags.put(PROCESSED, "true");
        blobClient.setTags(tags);
        LOGGER.info("setTags done for {}", blobName);
        return true;
    }
}
