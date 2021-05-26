package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DefaultHearingRecordingStorage implements HearingRecordingStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHearingRecordingStorage.class);
    private static final int BLOB_LIST_TIMEOUT = 5;
    private final BlobContainerAsyncClient hrsBlobContainerAsyncClient;
    private final BlobContainerClient hrsBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;


    private final String cvpConnectionString;

    @Inject
    public DefaultHearingRecordingStorage(final BlobContainerAsyncClient hrsContainerAsyncClient,
                                          final @Named("HrsBlobContainerClient") BlobContainerClient hrsContainerClient,
                                          final @Named("CvpBlobContainerClient") BlobContainerClient cvpContainerClient,

                                          @Value("${azure.storage.cvp.connection-string}") String cvpConnectionString) {
        this.hrsBlobContainerAsyncClient = hrsContainerAsyncClient;
        this.hrsBlobContainerClient = hrsContainerClient;
        this.cvpBlobContainerClient = cvpContainerClient;
        this.cvpConnectionString = cvpConnectionString;
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
    public void copyRecording(String sourceUri, final String filename) {

        BlockBlobClient destinationBlobClient = hrsBlobContainerClient.getBlobClient(filename).getBlockBlobClient();

        LOGGER.info("############## Trying copy from URL for sourceUri {}", sourceUri);

        //TODO should we compare md5sum of destination as well or
        // Or always overwrite (assume ingestor knows if it should be replaced or not, so md5 checksum done there)?
        if (!destinationBlobClient.exists()) {
            if (CvpConnectionResolver.isACvpEndpointUrl(cvpConnectionString)) {
                String sasToken = generateReadSasForCvp(filename);
                sourceUri = sourceUri + "?" + sasToken;
            }


            try {

                BlockBlobClient sourceBlob = cvpBlobContainerClient.getBlobClient(filename).getBlockBlobClient();
                LOGGER.debug("sourceBlob.exists() {}", sourceBlob.exists());

                SyncPoller<BlobCopyInfo, Void> poller = destinationBlobClient.beginCopy(sourceUri, null);
                PollResponse<BlobCopyInfo> poll = poller.waitForCompletion();
                LOGGER.info("File copy completed for {} with status {}", sourceUri, poll.getStatus());
            } catch (BlobStorageException be) {
                LOGGER.info("Blob Copy BlobStorageException code {}, message{}", be.getErrorCode(), be.getMessage());
                throw new BlobCopyException(be.getMessage(), be);
                //TODO should we try and clean up the destination blob? can it be partially present?
            } catch (Exception e) {
                LOGGER.info("Unhandled Exception during Blob Copy {}", e.getMessage());
                throw new BlobCopyException(e.getMessage(), e);
                //TODO should we try and clean up the destination blob? can it be partially present?
            }


        } else {
            LOGGER.info("############## target blobstore already has file: {}", filename);
        }

    }


    private String generateReadSasForCvp(String fileName) {

        LOGGER.debug("Attempting to generate SAS for container name {}", cvpBlobContainerClient.getBlobContainerName());

        BlobServiceClient blobServiceClient = cvpBlobContainerClient.getServiceClient();

        if (CvpConnectionResolver.isACvpEndpointUrl(cvpConnectionString)) {
            LOGGER.info("Getting a fresh MI token for Blob Service Client");
            BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

            builder.endpoint(cvpConnectionString);
            builder.credential(credential);
            blobServiceClient = builder.buildClient();
        }

        // get User Delegation Key - TODO consider optimising user key delegation usage to be hourly or daily with a
        //  lazy cache
        LOGGER.info("Getting User Delegation Key using BlobServiceClient with long offset times");
        OffsetDateTime delegationKeyStartTime = OffsetDateTime.now().minusMinutes(95);
        OffsetDateTime delegationKeyExpiryTime = OffsetDateTime.now().plusMinutes(95);
        UserDelegationKey
            key = blobServiceClient.getUserDelegationKey(delegationKeyStartTime, delegationKeyExpiryTime);

        //get SAS String for blobfile
        LOGGER.info("get SAS String using BlobClient for blobfile: {}", fileName);

        BlobClient sourceBlob = cvpBlobContainerClient.getBlobClient(fileName);
        // generate sas token
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(95);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true).setListPermission(true);

        BlobServiceSasSignatureValues myValues = new BlobServiceSasSignatureValues(expiryTime, permission)
            .setStartTime(OffsetDateTime.now().minusMinutes(95));
        String sas = sourceBlob.generateUserDelegationSas(myValues, key);

        return sas;


    }

}
