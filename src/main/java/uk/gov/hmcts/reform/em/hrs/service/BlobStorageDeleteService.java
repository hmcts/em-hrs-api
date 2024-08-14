package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.specialized.BlockBlobClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class BlobStorageDeleteService {

    private final BlobContainerClient cvpBlobContainerClient;
    private final BlobContainerClient vhBlobContainerClient;


    @Autowired
    public BlobStorageDeleteService(
        @Qualifier("CvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient,
        @Qualifier("vhBlobContainerClient") BlobContainerClient vhCloudBlobContainer) {
        this.cvpBlobContainerClient = cvpBlobContainerClient;
        vhBlobContainerClient = vhCloudBlobContainer;
    }

    public void deleteCvpBlob(String blobName) {
        deleteBlob(blobName, cvpBlobContainerClient);
    }

    public void deleteVhBlob(String blobName) {
        deleteBlob(blobName, vhBlobContainerClient);
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
