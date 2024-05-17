package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Configuration;
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
import com.azure.storage.blob.sas.BlobContainerSasPermission;
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
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.exception.BlobNotFoundException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import static com.azure.core.util.polling.LongRunningOperationStatus.SUCCESSFULLY_COMPLETED;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.extractAccountFromUrl;

@SuppressWarnings("squid:S2139")
@Component
public class HearingRecordingStorageImpl implements HearingRecordingStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingRecordingStorageImpl.class);
    private static final int BLOB_LIST_TIMEOUT = 5;
    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(3);

    private static final int COUNT_LAST_89_DAYS = 89;
    private final BlobContainerClient hrsCvpBlobContainerClient;
    private final BlobContainerClient hrsVhBlobContainerClient;
    private final BlobContainerClient cvpBlobContainerClient;
    private final BlobContainerClient vhContainerClient;
    private final String cvpConnectionString;
    private final String vhConnectionString;
    private final boolean useAdAuth;

    @Value("${vh.enable-report}")
    private boolean enableVhReport;

    @Autowired
    public HearingRecordingStorageImpl(
        final @Qualifier("hrsCvpBlobContainerClient") BlobContainerClient hrsCvpContainerClient,
        final @Qualifier("hrsVhBlobContainerClient") BlobContainerClient hrsVhContainerClient,
        final @Qualifier("CvpBlobContainerClient") BlobContainerClient cvpContainerClient,
        final @Qualifier("vhBlobContainerClient") BlobContainerClient vhContainerClient,
        @Value("${azure.storage.cvp.connection-string}") String cvpConnectionString,
        @Value("${azure.storage.vh.connection-string}") String vhConnectionString,
        @Value("${azure.storage.use-ad-auth}") boolean useAdAuth
    ) {
        this.hrsCvpBlobContainerClient = hrsCvpContainerClient;
        this.hrsVhBlobContainerClient = hrsVhContainerClient;
        this.cvpBlobContainerClient = cvpContainerClient;
        this.vhContainerClient = vhContainerClient;
        this.cvpConnectionString = cvpConnectionString;
        this.vhConnectionString = vhConnectionString;
        this.useAdAuth = useAdAuth;
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

        final PagedIterable<BlobItem> blobItems = hrsCvpBlobContainerClient.listBlobs(options, duration);

        return blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(BlobItem::getName))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public BlobDetail findBlob(final HearingSource hearingSource, final String blobName) {

        BlobClient blobClient;
        if (hearingSource == HearingSource.VH) {
            blobClient = hrsVhBlobContainerClient.getBlobClient(blobName);
        } else if (hearingSource == HearingSource.CVP) {
            blobClient = hrsCvpBlobContainerClient.getBlobClient(blobName);
        } else {
            throw new BlobNotFoundException("hearingSource", "" + hearingSource);
        }

        if (!blobClient.exists()) {
            throw new BlobNotFoundException("blobName", blobName);
        }
        var prop = blobClient.getProperties();

        return new BlobDetail(blobClient.getBlobUrl(), prop.getBlobSize(), prop.getLastModified());
    }

    public record BlobDetail(String blobUrl, long blobSize, OffsetDateTime lastModified) {
    }

    @Override
    public void copyRecording(HearingRecordingDto hrDto) {

        String sourceUri = hrDto.getSourceBlobUrl();
        String filename = hrDto.getFilename();
        HearingSource recordingSource = hrDto.getRecordingSource();

        try {
            var containersToCopy = getCopyContainers(filename, recordingSource);
            BlockBlobClient destinationBlobClient = containersToCopy.destination;
            BlockBlobClient sourceBlob = containersToCopy.source;

            LOGGER.info(
                "########## Trying copy from URL for sourceUri:{}, to destination:{}",
                sourceUri,
                destinationBlobClient.getBlobUrl()
            );
            if (Boolean.FALSE.equals(destinationBlobClient.exists())
                || destinationBlobClient.getProperties().getBlobSize() == 0) {
                if (useAdAuth) {
                    LOGGER.info("Generating and appending SAS token for copy for filename{}", filename);
                    String sasToken = generateReadSas(filename, recordingSource);
                    sourceUri = sourceUri + "?" + sasToken;
                    LOGGER.info("Generated SasToken {}", sasToken);
                } else {

                    String sasToken = sourceBlob
                        .generateSas(
                            new BlobServiceSasSignatureValues(
                                OffsetDateTime.of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
                                new BlobContainerSasPermission().setReadPermission(true)
                            )
                        );

                    sourceUri = sourceUri + "?" + sasToken;
                    LOGGER.info("Generated sourceUri {}", sourceUri);
                }

                LOGGER.info("SAS token created for filename{}", filename);
                PollResponse<BlobCopyInfo> poll = null;
                try {
                    LOGGER.info(
                        "file name {}, exists {}",
                        filename,
                        sourceBlob.exists()
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

    private record BlobClientsForCopy(BlockBlobClient source, BlockBlobClient destination) {
    }

    private BlobClientsForCopy getCopyContainers(String fileName, HearingSource recordingSource) {
        switch (recordingSource) {
            case CVP:
                BlockBlobClient sourceBlobClient =
                    cvpBlobContainerClient.getBlobClient(fileName).getBlockBlobClient();
                BlockBlobClient destinationBlobClient =
                    hrsCvpBlobContainerClient.getBlobClient(fileName).getBlockBlobClient();
                return new BlobClientsForCopy(sourceBlobClient, destinationBlobClient);
            case VH:
                sourceBlobClient =
                    vhContainerClient.getBlobClient(fileName).getBlockBlobClient();
                destinationBlobClient =
                    hrsVhBlobContainerClient.getBlobClient(fileName).getBlockBlobClient();
                return new BlobClientsForCopy(sourceBlobClient, destinationBlobClient);
            default:
                throw new RuntimeException("Source Container" + recordingSource + "not Found");
        }
    }

    private String generateReadSas(String fileName, HearingSource recordingSource) {
        if (HearingSource.CVP == recordingSource) {
            return generateReadSas(fileName, this.cvpBlobContainerClient, this.cvpConnectionString);
        } else {
            return generateReadSas(fileName, this.vhContainerClient, this.vhConnectionString);
        }
    }

    private String generateReadSas(String fileName, BlobContainerClient blobContainerClient, String connectionString) {

        LOGGER.debug("Attempting to generate SAS for container name {}", blobContainerClient.getBlobContainerName());

        BlobServiceClient blobServiceClient = blobContainerClient.getServiceClient();

        if (useAdAuth) {
            LOGGER.info("Getting a fresh MI token for Blob Service Client");
            BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            Configuration configuration = Configuration.getGlobalConfiguration().clone();
            var tenantId = configuration.get(Configuration.PROPERTY_AZURE_TENANT_ID);
            var managedIdentityClientId = configuration.get(Configuration.PROPERTY_AZURE_CLIENT_ID);
            LOGGER.info("Configuration tenantId {}, managedIdentityClientId {}", tenantId, managedIdentityClientId);
            builder.endpoint(connectionString);
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

        BlobClient sourceBlob = blobContainerClient.getBlobClient(fileName);
        // generate sas token
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(95);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);

        BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(expiryTime, permission)
            .setStartTime(OffsetDateTime.now().minusMinutes(95));
        String accountName =
            extractAccountFromUrl(connectionString);//TODO this is hardcoded for perftest enviro
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
        Set cvpItems = cvpBlobItems
            .stream()
            .filter(blobItem -> blobItem.getName().contains("/") && blobItem.getName().contains(".mp"))
            .filter(
                blobItem -> {
                    OffsetDateTime creationTime = blobItem.getProperties().getCreationTime();
                    if (isCreatedToday(creationTime, today)) {
                        cvpTodayItemCounter.count++;
                    }
                    return creationTime.isAfter(
                        OffsetDateTime.of(
                            LocalDate.now().minusDays(COUNT_LAST_89_DAYS),
                            LocalTime.MIDNIGHT,
                            ZoneOffset.UTC
                        ));
                }
            )
            .map(blb -> blb.getName())
            .collect(Collectors.toSet());

        var hrsCvpTodayItemCounter = new Counter();
        long cvpItemCount = cvpItems.size();
        long hrsCvpItemCount = hrsCvpBlobContainerClient.listBlobs(options, duration)
            .stream()
            .filter(blobItem -> blobItem.getName().contains("/"))
            .filter(
                blobItem -> {
                    OffsetDateTime creationTime = blobItem.getProperties().getCreationTime();
                    cvpItems.remove(blobItem.getName());
                    if (isCreatedToday(creationTime, today)) {
                        hrsCvpTodayItemCounter.count++;
                    }
                    return creationTime.isAfter(
                        OffsetDateTime.of(
                            LocalDate.now().minusDays(COUNT_LAST_89_DAYS),
                            LocalTime.MIDNIGHT,
                            ZoneOffset.UTC
                        ));
                }
            ).count();
        LOGGER.info("CVP-HRS difference {}", cvpItems);
        LOGGER.info(
            "StorageReport CVP Total Count= {} vs HRS Total Count= {}, Today CVP= {} vs HRS= {} ",
            cvpItemCount,
            hrsCvpItemCount,
            cvpTodayItemCounter.count,
            hrsCvpTodayItemCounter.count
        );
        var vhTodayItemCounter = new Counter();
        long vhTotalCount = 0;
        var hrsVhTodayItemCounter = new Counter();
        long hrsVhItemCount = 0;
        if (enableVhReport) {
            final PagedIterable<BlobItem> vhBlobItems = vhContainerClient.listBlobs(options, duration);


            Set vhItems = vhBlobItems
                .stream()
                .filter(blobItem -> blobItem.getName().contains(".mp"))
                .peek(blob -> {
                    OffsetDateTime creationTime = blob.getProperties().getCreationTime();
                    if (isCreatedToday(creationTime, today)) {
                        vhTodayItemCounter.count++;
                    }
                })
                .map(blb -> blb.getName())
                .collect(Collectors.toSet());
            vhTotalCount = vhItems.size();
            LOGGER.info("VH count vhTotalCount {} vhTodayItemCounter{}", vhTotalCount, vhTodayItemCounter.count);
            hrsVhItemCount = hrsVhBlobContainerClient.listBlobs(options, duration)
                .stream()
                .peek(
                    blobItem -> {
                        vhItems.remove(blobItem.getName());
                        OffsetDateTime creationTime = blobItem.getProperties().getCreationTime();
                        if (isCreatedToday(creationTime, today)) {
                            hrsVhTodayItemCounter.count++;
                        }
                    }
                ).count();
            LOGGER.info("StorageReport VH difference {} ", vhItems);
        }

        LOGGER.info(
            "StorageReport VH Total Count= {} vs HRS VH Total Count= {}, Today VH= {} vs HRS= {} ",
            vhTotalCount,
            hrsVhItemCount,
            vhTodayItemCounter.count,
            hrsVhTodayItemCounter.count
        );

        return new StorageReport(
            today,
            new StorageReport.HrsSourceVsDestinationCounts(
                cvpItemCount,
                hrsCvpItemCount,
                cvpTodayItemCounter.count,
                hrsCvpTodayItemCounter.count
            ),
            new StorageReport.HrsSourceVsDestinationCounts(
                vhTotalCount,
                hrsVhItemCount,
                vhTodayItemCounter.count,
                hrsVhTodayItemCounter.count
            )
        );
    }

    private boolean isCreatedToday(OffsetDateTime creationTime, LocalDate today) {
        return creationTime.isAfter(
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


