package uk.gov.hmcts.reform.em.hrs.service.impl;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.service.BlobStorageDeleteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class BlobStorageDeleteServiceTest {

    @Mock
    private BlobContainerClient vhBlobContainer;

    @Mock
    private BlobContainerClient cvpBlobContainer;

    @Spy
    @InjectMocks
    private BlobStorageDeleteService blobStorageDeleteService;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlockBlobClient blob;

    @Mock
    private Response<Void> mockResponse;

    private String blobName;


    @BeforeEach
    void setUp() {
        lenient().when(cvpBlobContainer.getBlobClient(any())).thenReturn(blobClient);
        lenient().when(vhBlobContainer.getBlobClient(any())).thenReturn(blobClient);
        lenient().when(blobClient.getBlockBlobClient()).thenReturn(blob);

        blobName = RandomStringUtils.randomAlphanumeric(9);
    }

    @Test
    void deleteCvpBlob() {
        when(mockResponse.getStatusCode()).thenReturn(202);
        given(blob.exists()).willReturn(true);
        given(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .willReturn(mockResponse);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService, never()).logDeletionFailure(blobName, 202);
    }

    @Test
    void deleteVhBlob() {
        when(mockResponse.getStatusCode()).thenReturn(202);
        given(blob.exists()).willReturn(true);
        given(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .willReturn(mockResponse);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.VH);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService, never()).logDeletionFailure(blobName, 202);
    }

    @Test
    void deleteBlobResponseCode404() {
        when(mockResponse.getStatusCode()).thenReturn(404);
        when(blob.exists()).thenReturn(true);
        when(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .thenReturn(mockResponse);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService, never()).logDeletionFailure(blobName, 404);
    }

    @Test
    void deleteBlobResponseCodeNot202or404() {
        when(mockResponse.getStatusCode()).thenReturn(409);
        when(blob.exists()).thenReturn(true);
        when(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .thenReturn(mockResponse);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService).logDeletionFailure(blobName, 409);
    }

    @Test
    void deleteBlobException() {
        var blobStorageException = mock(BlobStorageException.class);
        when(blob.exists()).thenReturn(true);
        when(blobStorageException.getStatusCode()).thenReturn(409);
        when(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .thenThrow(blobStorageException);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService).logDeletionFailure(blobName, 409);
    }

    @Test
    void deleteBlob404Exception() {
        var blobStorageException = mock(BlobStorageException.class);
        when(blob.exists()).thenReturn(true);
        when(blobStorageException.getStatusCode()).thenReturn(404);
        when(blob.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null))
            .thenThrow(blobStorageException);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob).deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
        verify(blobStorageDeleteService, never()).logDeletionFailure(blobName, 404);
    }

    @Test
    void deleteBlobNotExists() {
        when(blob.exists()).thenReturn(false);
        blobStorageDeleteService.deleteBlob(blobName, HearingSource.CVP);
        verify(blob, never()).deleteWithResponse(any(), any(), any(), any());
    }

}

