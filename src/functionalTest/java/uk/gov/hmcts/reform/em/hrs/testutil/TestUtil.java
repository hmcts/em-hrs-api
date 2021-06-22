package uk.gov.hmcts.reform.em.hrs.testutil;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.BaseTest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

@Component
public class TestUtil {

    private final BlobContainerClient hrsBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    @Autowired
    public TestUtil(@Qualifier("hrsBlobContainerClient") BlobContainerClient hrsBlobContainerClient,
                    @Qualifier("cvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient) {
        this.hrsBlobContainerClient = hrsBlobContainerClient;
        this.cvpBlobContainerClient = cvpBlobContainerClient;
    }

    public void deleteFileFromHrsContainer(final String folderName) {
        hrsBlobContainerClient.listBlobs()
            .stream()
            .filter(blobItem -> blobItem.getName().startsWith(folderName))
            .forEach(blobItem -> {
                final BlockBlobClient blobClient = hrsBlobContainerClient.getBlobClient(blobItem.getName()).getBlockBlobClient();
                blobClient.delete();
            });
    }

    public int checkIfUploadedToCvp(final String folderName) {
        LOGGER.info("cvpBlobContainerClient.getBlobContainerUrl() ~{}", cvpBlobContainerClient.getBlobContainerUrl());
        int count = (int) cvpBlobContainerClient.listBlobs()
            .stream()
            .filter(blobItem -> blobItem.getName().startsWith(folderName)).count();
        return count;
    }

//    public int checkIfUploadedToHrs(final String folderName) {
//        LOGGER.info("hrsBlobContainerClient.getBlobContainerUrl() ~{}", hrsBlobContainerClient.getBlobContainerUrl());
//        int count = (int) hrsBlobContainerClient.listBlobs()
//            .stream()
//            .filter(blobItem -> blobItem.getName().startsWith(folderName)).count();
//        return count;
//    }

    public void deleteFileFromCvpContainer(final String folderName) {
        cvpBlobContainerClient.listBlobs()
            .stream()
            .filter(blobItem -> blobItem.getName().startsWith(folderName))
            .forEach(blobItem -> {
                final BlockBlobClient blobClient = cvpBlobContainerClient.getBlobClient(blobItem.getName()).getBlockBlobClient();
                blobClient.delete();
            });
    }

    public void uploadToCvpContainer(final String blobName) throws Exception {
        final FileInputStream fileInputStream = getTestFile();
        final byte[] bytes = fileInputStream.readAllBytes();
        final InputStream inStream = new ByteArrayInputStream(bytes);

        final BlobClient blobClient = cvpBlobContainerClient.getBlobClient(blobName);
        LOGGER.info("cvpBlobContainerClient.getBlobContainerUrl() ~{}", cvpBlobContainerClient.getBlobContainerUrl());
        blobClient.upload(new BufferedInputStream(inStream), bytes.length);
    }

    public FileInputStream getTestFile() throws Exception {
        final URL resource = TestUtil.class.getClassLoader().getResource("data/test_data.mp4");
        final File file = new File(Objects.requireNonNull(resource).toURI());
        return new FileInputStream(file);
    }
}
