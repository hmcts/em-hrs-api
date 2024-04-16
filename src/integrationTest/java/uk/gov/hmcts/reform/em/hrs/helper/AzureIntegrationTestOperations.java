package uk.gov.hmcts.reform.em.hrs.helper;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.devskiller.jfairy.Fairy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AzureIntegrationTestOperations {
    private static final int BLOB_LIST_TIMEOUT = 5;
    private final BlobContainerClient hrsCvpBlobContainerClient;

    private final BlobContainerClient hrsVhBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;
    private final BlobContainerClient vhBlobContainerClient;
    private final Fairy fairy;

    @Autowired
    public AzureIntegrationTestOperations(
        final @Qualifier("HrsCvpBlobContainerClient") BlobContainerClient hrsCvpBlobContainerClient,
        final @Qualifier("HrsVhBlobContainerClient") BlobContainerClient hrsVhBlobContainerClient,
        final @Qualifier("CvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient,
        final @Qualifier("vhBlobContainerClient") BlobContainerClient vhBlobContainerClient
    ) {
        this.hrsCvpBlobContainerClient = hrsCvpBlobContainerClient;
        this.hrsVhBlobContainerClient = hrsVhBlobContainerClient;
        this.cvpBlobContainerClient = cvpBlobContainerClient;
        this.vhBlobContainerClient = vhBlobContainerClient;
        fairy = Fairy.create();
    }

    public void populateHrsContainer(final Set<String> filePaths) {
        filePaths.forEach(this::uploadToHrsContainer);
    }

    public String populateHrsCvpContainer(final String blobName, final String content) {
        return uploadToHrsCvpContainer(blobName, content.getBytes(StandardCharsets.UTF_8));
    }

    public void populateHrsVhContainer(final String blobName, final String content) {
        uploadToHrsVhContainer(blobName, content.getBytes(StandardCharsets.UTF_8));
    }

    public void populateCvpContainer(final Set<String> filePaths) {
        filePaths.forEach(this::uploadToCvpContainer);
    }

    public void uploadToHrsContainer(final String filePath) {
        uploadToContainer(Container.HRS, filePath);
    }

    private String uploadToHrsCvpContainer(final String blobName, final byte[] data) {
        final InputStream inStream = new ByteArrayInputStream(data);

        final BlobClient blobClient = hrsCvpBlobContainerClient.getBlobClient(blobName);
        blobClient.upload(new BufferedInputStream(inStream), data.length);
        return blobClient.getBlobUrl();
    }

    private void uploadToHrsVhContainer(final String blobName, final byte[] data) {
        final InputStream inStream = new ByteArrayInputStream(data);

        final BlobClient blobClient = hrsVhBlobContainerClient.getBlobClient(blobName);
        blobClient.upload(new BufferedInputStream(inStream), data.length);
    }


    public void uploadToCvpContainer(final String filePath) {
        uploadToContainer(Container.CVP, filePath);
    }

    private void uploadToContainer(final Enum<Container> container, final String filePath) {
        final String content = fairy.textProducer().sentence();
        final InputStream data = new ByteArrayInputStream(content.getBytes());

        final BlobClient blobClient = container.equals(Container.CVP)
            ? cvpBlobContainerClient.getBlobClient(filePath)
            : hrsCvpBlobContainerClient.getBlobClient(filePath);
        blobClient.upload(new BufferedInputStream(data), content.length());
    }

    public String getBlobUrl(final String filename) {
        final BlockBlobClient srcBlobClient = cvpBlobContainerClient.getBlobClient(filename).getBlockBlobClient();
        return srcBlobClient.getBlobUrl();
    }

    public Set<String> getHrsBlobsFrom(final String folder) {
        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails)
            .setPrefix(folder);
        final Duration duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> blobItems = hrsCvpBlobContainerClient.listBlobs(options, duration);

        return blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(BlobItem::getName))
            .collect(Collectors.toUnmodifiableSet());
    }

    public void clearContainer() {
        hrsCvpBlobContainerClient.listBlobs()
            .forEach(x -> x.setDeleted(true));
    }

    private enum Container {
        CVP,
        HRS
    }
}
