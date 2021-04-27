package uk.gov.hmcts.reform.em.hrs.testutil;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired
    public TestUtil(BlobContainerClient hrsBlobContainerClient,
                    BlobContainerClient cvpBlobContainerClient) {
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
        blobClient.upload(new BufferedInputStream(inStream), bytes.length);
    }

    public FileInputStream getTestFile() throws Exception {
        final URL resource = TestUtil.class.getClassLoader().getResource("data/test_data.mp4");
        final File file = new File(Objects.requireNonNull(resource).toURI());
        final FileInputStream fileInputStream = new FileInputStream(file);
        return fileInputStream;
    }
}
