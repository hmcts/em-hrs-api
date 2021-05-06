package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.options.BlobBeginCopyOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

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
    private final Snooper snooper;

    private final String cvpConnectionString;

    @Inject
    public DefaultHearingRecordingStorage(final BlobContainerAsyncClient hrsContainerAsyncClient,
                                          final @Named("HrsBlobContainerClient") BlobContainerClient hrsContainerClient,
                                          final @Named("CvpBlobContainerClient") BlobContainerClient cvpContainerClient,
                                          final Snooper snooper,
                                          @Value("${azure.storage.cvp.connection-string}") String cvpConnectionString) {
        this.hrsBlobContainerAsyncClient = hrsContainerAsyncClient;
        this.hrsBlobContainerClient = hrsContainerClient;
        this.cvpBlobContainerClient = cvpContainerClient;
        this.snooper = snooper;
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
        LOGGER.info("**************************************");
        LOGGER.info("**************************************");
        LOGGER.info("**************************************");
        LOGGER.info("About to Copy Recording for filename {}", filename);
        if (CvpConnectionResolver.isACvpEndpointUrl(cvpConnectionString)) {

            LOGGER.info("Generating sasToken");
            String sasToken = generateReadSASForCVP(filename);
            sourceUri = sourceUri + "?" + sasToken;


            LOGGER.info("overwriting URL with hardcoded prefix to overcome / to %2f encoding...");

            sourceUri = "https://cvprecordingsstgsa.blob.core.windows.net/recordings/" + filename + "?" + sasToken;
        }

        LOGGER.info("Source URI {}", sourceUri);

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


    private String generateReadSASForCVP(String fileName) {

        LOGGER.info("Attempting to generate SAS");

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
