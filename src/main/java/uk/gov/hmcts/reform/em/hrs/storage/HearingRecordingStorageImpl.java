package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import static com.azure.core.util.polling.LongRunningOperationStatus.SUCCESSFULLY_COMPLETED;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.extractAccountFromUrl;

@SuppressWarnings("squid:S2139")
@Component
public class HearingRecordingStorageImpl implements HearingRecordingStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingRecordingStorageImpl.class);
    private static final int BLOB_LIST_TIMEOUT = 5;
    private final BlobContainerClient hrsBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;


    private final String cvpConnectionString;
    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(3);

    @Autowired
    public HearingRecordingStorageImpl(
        final @Qualifier("HrsBlobContainerClient") BlobContainerClient hrsContainerClient,
        final @Qualifier("CvpBlobContainerClient") BlobContainerClient cvpContainerClient,
        @Value("${azure.storage.cvp.connection-string}") String cvpConnectionString
    ) {
        this.hrsBlobContainerClient = hrsContainerClient;
        this.cvpBlobContainerClient = cvpContainerClient;
        this.cvpConnectionString = cvpConnectionString;
    }

    @Override
    public Set<String> findByFolderName(final String folderName) {
        boolean folderNameIncludesTrailingSlash = StringUtils.endsWith(folderName, "/");

        var folderPath = folderNameIncludesTrailingSlash ? folderName : folderName + "/";

        var blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        var options = new ListBlobsOptions()
            .setDetails(blobListDetails)
            .setPrefix(folderPath);
        final var duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> blobItems = hrsBlobContainerClient.listBlobs(options, duration);

        return blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(BlobItem::getName))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void copyRecording(String sourceUri, final String filename) {

        try {
            BlockBlobClient destinationBlobClient = hrsBlobContainerClient.getBlobClient(filename).getBlockBlobClient();

            LOGGER.info("########## Trying copy from URL for sourceUri {}", sourceUri);
            if (Boolean.FALSE.equals(destinationBlobClient.exists())
                || destinationBlobClient.getProperties().getBlobSize() == 0) {

                if (CvpConnectionResolver.isACvpEndpointUrl(cvpConnectionString)) {
                    LOGGER.info("Generating and appending SAS token for copy for filename{}", filename);
                    String sasToken = generateReadSasForCvp(filename);
                    sourceUri = sourceUri + "?" + sasToken;
                    LOGGER.info("Generated SasToken {}", sasToken);

                }

                LOGGER.info("SAS token created for filename{}", filename);
                PollResponse<BlobCopyInfo> poll = null;
                try {

                    LOGGER.info("get cvpBlobContainerClient for filename {}", filename);

                    LOGGER.info(
                        "file name {}, getBlobContainerName {}, exists {}",
                        filename,
                        cvpBlobContainerClient.getBlobContainerName(),
                        cvpBlobContainerClient.exists()
                    );
                    SyncPoller<BlobCopyInfo, Void> poller = destinationBlobClient.beginCopy(
                        sourceUri,
                        POLLING_INTERVAL
                    );
                    LOGGER.info("Wait For Completion filename {}", filename);

                    poll = poller.waitForCompletion();
                    LOGGER.info(
                        "File copy completed for {} with status {}",
                        filename,
                        poll.getStatus()
                    );
                } catch (BlobStorageException be) {
                    LOGGER.error(
                        "Blob Copy BlobStorageException code {}, message{}, file {}",
                        be.getErrorCode(),
                        be.getMessage(),
                        filename
                    );
                    if (poll != null) {
                        try {
                            destinationBlobClient.abortCopyFromUrl(poll.getValue().getCopyId());
                        } catch (Exception exc) {
                            LOGGER.error(
                                "Abort Copy From Url got Error,  for {}  to rejected container",
                                filename,
                                exc
                            );
                        }
                    }
                    LOGGER.info("Delete if exist {} ", filename);
                    destinationBlobClient.deleteIfExists();
                    throw new BlobCopyException(be.getMessage(), be);
                }

                if (poll != null && !SUCCESSFULLY_COMPLETED.equals(poll.getStatus())) {
                    destinationBlobClient.deleteIfExists();
                    throw new BlobCopyException("Copy not completed successfully");
                }
            } else {
                LOGGER.info("############## target blobstore already has file: {}", filename);
            }
        } catch (Exception e) {
            LOGGER.error(
                "Unhandled Exception During Blob Copy Process {}, filename {}",
                e.getMessage(),
                filename
            );
            throw new BlobCopyException(e.getMessage(), e);
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
            userDelegationKey = blobServiceClient.getUserDelegationKey(delegationKeyStartTime, delegationKeyExpiryTime);

        //get SAS String for blobfile
        LOGGER.info("get SAS String using BlobClient for blobfile: {}", fileName);

        BlobClient sourceBlob = cvpBlobContainerClient.getBlobClient(fileName);
        // generate sas token
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(95);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true).setListPermission(true);

        BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(expiryTime, permission)
            .setStartTime(OffsetDateTime.now().minusMinutes(95));
        String accountName =
            extractAccountFromUrl(cvpConnectionString);//TODO this is hardcoded for perftest enviro
        LOGGER.info("GenerateUserDelegationSas for blobfile: {}", fileName);
        return sourceBlob.generateUserDelegationSas(signatureValues, userDelegationKey, accountName, Context.NONE);
    }

    @Override
    public synchronized StorageReport getStorageReport() {
        LOGGER.info("StorageReport Creating storage report");
        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails);
        final Duration duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> cvpBlobItems = cvpBlobContainerClient.listBlobs(options, duration);

        LocalDate today = LocalDate.now();

        var cvpTodayItemCounter = new Counter();
        Set cvpItems  = cvpBlobItems
            .stream()
            .filter(blobItem -> blobItem.getName().contains("/") && blobItem.getName().contains(".mp"))
            .peek(blob -> {
                if (isCreatedToday(blob, today)) {
                    cvpTodayItemCounter.count++;
                }
            })
            .map(blb -> blb.getName())
            .collect(Collectors.toSet());


        long cvpItemCount = cvpItems.size();
        LOGGER.info("StorageReport CVP done");

        final BlobListDetails hrsBlobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions hrsOptions = new ListBlobsOptions()
            .setDetails(hrsBlobListDetails);
        var hrsTodayItemCounter = new Counter();

        long hrsItemCount = hrsBlobContainerClient.listBlobs(hrsOptions, duration)
            .stream()
            .filter(blobItem -> blobItem.getName().contains("/"))
            .peek(blob -> {
                cvpItems.remove(blob.getName());
                if (isCreatedToday(blob, today)) {
                    hrsTodayItemCounter.count++;
                }
            }).count();

        LOGGER.info("CVP-HRS difference {}", cvpItems);
        LOGGER.info(
            "StorageReport CVP Total Count= {} vs HRS Total Count= {}, Today CVP= {} vs HRS= {} ",
            cvpItemCount,
            hrsItemCount,
            cvpTodayItemCounter.count,
            hrsTodayItemCounter.count
        );
        return new StorageReport(
            today,
            cvpItemCount,
            hrsItemCount,
            cvpTodayItemCounter.count,
            hrsTodayItemCounter.count
        );
    }

    private boolean isCreatedToday(BlobItem blobItem, LocalDate today) {
        return blobItem.getProperties().getCreationTime().isAfter(
            OffsetDateTime.of(
                today,
                LocalTime.MIDNIGHT,
                ZoneOffset.UTC
            ));
    }

    private class Counter {
        public long count = 0;
    }
}


