package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class BlobstoreClientImpl implements BlobstoreClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobstoreClientImpl.class);

    private final BlobContainerClient hrsCvpBlobContainerClient;
    private final BlobContainerClient hrsVhBlobContainerClient;

    @Autowired
    public BlobstoreClientImpl(
        @Qualifier("HrsCvpBlobContainerClient") final BlobContainerClient hrsCvpBlobContainerClient,
        @Qualifier("HrsVhBlobContainerClient") final BlobContainerClient hrsVhBlobContainerClient
    ) {
        this.hrsCvpBlobContainerClient = hrsCvpBlobContainerClient;
        this.hrsVhBlobContainerClient = hrsVhBlobContainerClient;
    }

    @Override
    public BlobInfo fetchBlobInfo(String filename, String hearingSource) {
        final BlockBlobClient blobClient =
            getBlobContainerClient(hearingSource)
                .getBlobClient(filename)
                .getBlockBlobClient();
        final long fileSize = blobClient.getProperties().getBlobSize();
        final String contentType = blobClient.getProperties().getContentType();
        return new BlobInfo(fileSize, contentType);
    }

    @Override
    public void downloadFile(
        final String filename,
        BlobRange blobRange,
        final OutputStream outputStream,
        String hearingSource
    ) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        blockBlobClient(filename, hearingSource)
            .downloadStream(byteOutputStream);
        try {
            byteOutputStream.writeTo(outputStream);
        } catch (IOException e) {
            LOGGER.error("ByteArrayOutputStream failes", e);
        }
    }

    private BlockBlobClient blockBlobClient(String id, String hearingSource) {
        return getBlobContainerClient(hearingSource).getBlobClient(id).getBlockBlobClient();
    }

    private BlobContainerClient getBlobContainerClient(String hearingSource) {
        if (HearingSource.VH.name().equals(hearingSource)) {
            return this.hrsVhBlobContainerClient;
        }

        return this.hrsCvpBlobContainerClient;
    }

}
