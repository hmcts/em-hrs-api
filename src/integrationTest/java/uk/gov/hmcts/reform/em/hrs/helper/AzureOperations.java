package uk.gov.hmcts.reform.em.hrs.helper;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.devskiller.jfairy.Fairy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AzureOperations {
    private final BlobContainerClient hrsBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;
    private final Fairy fairy;

    private static final int BLOB_LIST_TIMEOUT = 5;

    private enum Container {
        CVP,
        HRS
    }

    @Inject
    public AzureOperations(final @Named("HrsBlobContainerClient") BlobContainerClient hrsBlobContainerClient,
                           final @Named("CvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient) {
        this.hrsBlobContainerClient = hrsBlobContainerClient;
        this.cvpBlobContainerClient = cvpBlobContainerClient;
        fairy = Fairy.create();
    }

    public void populateHrsContainer(final Set<String> filePaths) {
        filePaths.forEach(this::uploadToHrsContainer);
    }

    public void populateCvpContainer(final Set<String> filePaths) {
        filePaths.forEach(this::uploadToCvpContainer);
    }

    public void uploadToHrsContainer(final String filePath) {
        uploadToContainer(Container.HRS, filePath);
    }

    public void uploadToCvpContainer(final String filePath) {
        uploadToContainer(Container.CVP, filePath);
    }

    public void uploadToContainer(final Enum<Container> container, final String filePath) {
        final String content = fairy.textProducer().sentence();
        final InputStream data = new ByteArrayInputStream(content.getBytes());

        final BlobClient blobClient = container.equals(Container.CVP)
            ? cvpBlobContainerClient.getBlobClient(filePath)
            : hrsBlobContainerClient.getBlobClient(filePath);
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

        final PagedIterable<BlobItem> blobItems = hrsBlobContainerClient.listBlobs(options, duration);

        return blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(BlobItem::getName))
            .collect(Collectors.toUnmodifiableSet());
    }

    public void clearContainer() {
        hrsBlobContainerClient.listBlobs()
            .forEach(x -> x.setDeleted(true));
    }
}
