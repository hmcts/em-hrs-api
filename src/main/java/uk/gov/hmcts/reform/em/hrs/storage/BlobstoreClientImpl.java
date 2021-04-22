package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

@Component
public class BlobstoreClientImpl implements BlobstoreClient {

    private final BlobContainerClient blobContainerClient;

    @Autowired
    public BlobstoreClientImpl(@Qualifier("HrsBlobContainerClient") final BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public void downloadFile(final String filename, final OutputStream output) {
        final BlockBlobClient blobClient = blobContainerClient.getBlobClient(filename).getBlockBlobClient();
        blobClient.download(output);
    }
}
