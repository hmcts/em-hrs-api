package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.controller.HearingRecordingController;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;

@Service
public class BlobStorageDeleteService {

    private final BlobContainerClient cvpBlobContainerClient;
    private final BlobContainerClient vhBlobContainerClient;
    private final Logger log = LoggerFactory.getLogger(BlobStorageDeleteService.class);


    @Autowired
    public BlobStorageDeleteService(
        @Qualifier("hrsCvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient,
        @Qualifier("hrsVhBlobContainerClient") BlobContainerClient vhCloudBlobContainerClient) {
        this.cvpBlobContainerClient = cvpBlobContainerClient;
        this.vhBlobContainerClient = vhCloudBlobContainerClient;
    }

    public void deleteBlob(String blobName, HearingSource source) {
        switch (source) {
            case HearingSource.CVP -> deleteBlob(blobName, cvpBlobContainerClient);
            case HearingSource.VH -> deleteBlob(blobName, vhBlobContainerClient);
            default -> log.info("invalid blob source, deletion skipped");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void deleteBlob(String blobName, BlobContainerClient blobContainerClient) {

        try {
            BlockBlobClient blob =
                blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
            if (blob.exists()) {
                Response<Void> response = blob.deleteWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE, null, null, null);
                if (response.getStatusCode() != 202 && response.getStatusCode() != 404) {
                    log.info(
                        "Deleting hrs blob {} failed. Response status code {}",
                        blobName,
                        response.getStatusCode()
                    );
                    return;
                }
                log.info(
                    "Successfully deleted hrs blob: {}",
                    blob.getBlobUrl()
                );
            } else {
                log.info("blob does not exist: {}", blobName);
            }
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                log.info("Blob Not found for deletion {}", blobName);
            } else {
                log.info(
                    "Deleting hrs blob failed {},status {}",
                    blobName,
                    e.getStatusCode(),
                    e
                );
            }
        }
    }

}
