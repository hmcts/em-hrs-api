package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobBeginCopyOptions;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DefaultHearingRecordingStorage implements HearingRecordingStorage {
    private final BlobContainerAsyncClient hrsBlobContainerAsyncClient;
    private final BlobContainerClient hrsBlobContainerClient;
    private final Snooper snooper;

    private static final int BLOB_LIST_TIMEOUT = 5;

    @Inject
    public DefaultHearingRecordingStorage(final BlobContainerAsyncClient hrsContainerAsyncClient,
                                          final @Named("HrsBlobContainerClient") BlobContainerClient hrsContainerClient,
                                          final Snooper snooper) {
        this.hrsBlobContainerAsyncClient = hrsContainerAsyncClient;
        this.hrsBlobContainerClient = hrsContainerClient;
        this.snooper = snooper;
    }

    @Override
    public Set<String> findByFolder(final String folderName) {
        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails)
            .setPrefix(folderName);
        final Duration duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> blobItems = hrsBlobContainerClient.listBlobs(options, duration);

        return blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(BlobItem::getName))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void copyRecording(final String sourceUri, final String filename) {
        final BlobBeginCopyOptions blobBeginCopyOptions = new BlobBeginCopyOptions(sourceUri);
        final BlobAsyncClient destBlobAsyncClient = hrsBlobContainerAsyncClient.getBlobAsyncClient(filename);
        destBlobAsyncClient.beginCopy(blobBeginCopyOptions)
            .subscribe(
                x -> {
                },
                y -> snooper.snoop(String.format("File %s copied failed:: %s", filename, y.getMessage())),
                () -> snooper.snoop(String.format("File %s copied successfully", filename))
            );
    }

}
