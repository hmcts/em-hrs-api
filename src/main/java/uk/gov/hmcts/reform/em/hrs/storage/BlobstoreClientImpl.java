package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

@Component
public class BlobstoreClientImpl implements BlobstoreClient {

    private final BlobContainerClient blobContainerClient;

    @Autowired
    public BlobstoreClientImpl(@Qualifier("HrsCvpBlobContainerClient") final BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }


    @Override
    public BlobInfo fetchBlobInfo(String filename) {
        final BlockBlobClient blobClient = blobContainerClient.getBlobClient(filename).getBlockBlobClient();
        final long fileSize = blobClient.getProperties().getBlobSize();
        final String contentType = blobClient.getProperties().getContentType();
        return new BlobInfo(fileSize,contentType);
    }



    @Override
    public void downloadFile(final String filename, BlobRange blobRange,
                             final OutputStream outputStream) {

        blockBlobClient(filename)
            .downloadWithResponse(
                outputStream,
                blobRange,
                new DownloadRetryOptions().setMaxRetryRequests(5),
                null,
                false,
                null,
                null
            );
    }


    private BlockBlobClient blockBlobClient(String id) {
        return blobContainerClient.getBlobClient(id).getBlockBlobClient();
    }

}
