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

import java.io.IOException;
import java.io.OutputStream;

@Component
public class BlobstoreClientImpl implements BlobstoreClient {

    private final BlobContainerClient hrsCvpBlobContainerClient;
    private final BlobContainerClient hrsVhBlobContainerClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobstoreClientImpl.class);

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
    ) throws IOException {
        try (var blobStream = blockBlobClient(filename, hearingSource).openInputStream()) {
            LOGGER.info("Start downloadFile filename {}", filename);

            byte[] buf = new byte[8192];
            int length;
            long count = 0;
            while ((length = blobStream.read(buf)) != -1) {
                outputStream.write(buf, 0, length);
                buf = new byte[8192];
                count += length;
            }
            LOGGER.info("END downloadFile filename--> {},count:{}", filename, count);
        } catch (Exception e) {
            throw new IOException(e);
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
