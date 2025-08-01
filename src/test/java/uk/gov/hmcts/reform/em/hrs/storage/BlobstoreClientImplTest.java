package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobstoreClientImplTest {

    private static final String FILENAME = "test-file.mp4";
    private static final long FILE_SIZE = 1024L;
    private static final String CONTENT_TYPE = "video/mp4";

    @Mock
    private BlobContainerClient hrsCvpBlobContainerClient;
    @Mock
    private BlobContainerClient hrsVhBlobContainerClient;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlockBlobClient blockBlobClient;
    @Mock
    private BlobProperties blobProperties;

    private BlobstoreClientImpl blobstoreClient;

    @BeforeEach
    void setUp() {
        blobstoreClient = new BlobstoreClientImpl(hrsCvpBlobContainerClient, hrsVhBlobContainerClient);
    }

    @Test
    void fetchBlobInfoShouldUseCvpClientForCvpSource() {
        when(hrsCvpBlobContainerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        when(blockBlobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getBlobSize()).thenReturn(FILE_SIZE);
        when(blobProperties.getContentType()).thenReturn(CONTENT_TYPE);

        BlobInfo blobInfo = blobstoreClient.fetchBlobInfo(FILENAME, HearingSource.CVP.name());

        assertEquals(FILE_SIZE, blobInfo.getFileSize());
        assertEquals(CONTENT_TYPE, blobInfo.getContentType());
        verify(blockBlobClient, times(2)).getProperties();
        verify(hrsCvpBlobContainerClient).getBlobClient(FILENAME);
        verifyNoInteractions(hrsVhBlobContainerClient);
    }

    @Test
    void fetchBlobInfoShouldUseVhClientForVhSource() {
        when(hrsVhBlobContainerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        when(blockBlobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getBlobSize()).thenReturn(FILE_SIZE);
        when(blobProperties.getContentType()).thenReturn(CONTENT_TYPE);

        BlobInfo blobInfo = blobstoreClient.fetchBlobInfo(FILENAME, HearingSource.VH.name());

        assertEquals(FILE_SIZE, blobInfo.getFileSize());
        assertEquals(CONTENT_TYPE, blobInfo.getContentType());
        verify(blockBlobClient, times(2)).getProperties();
        verify(hrsVhBlobContainerClient).getBlobClient(FILENAME);
        verifyNoInteractions(hrsCvpBlobContainerClient);
    }

    @Test
    void downloadFileShouldUseCvpClientForCvpSource() {
        when(hrsCvpBlobContainerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);

        final BlobRange blobRange = new BlobRange(0, 1023L);
        final OutputStream outputStream = new ByteArrayOutputStream();

        blobstoreClient.downloadFile(FILENAME, blobRange, outputStream, HearingSource.CVP.name());

        verify(hrsCvpBlobContainerClient).getBlobClient(FILENAME);
        verify(blockBlobClient).downloadStreamWithResponse(
            eq(outputStream), eq(blobRange), any(DownloadRetryOptions.class),
            eq(null), eq(false), eq(null), eq(null)
        );
        verifyNoInteractions(hrsVhBlobContainerClient);
    }

    @Test
    void downloadFileShouldUseVhClientForVhSource() {
        when(hrsVhBlobContainerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);

        final BlobRange blobRange = new BlobRange(0, 500L);
        final OutputStream outputStream = new ByteArrayOutputStream();

        blobstoreClient.downloadFile(FILENAME, blobRange, outputStream, HearingSource.VH.name());

        verify(hrsVhBlobContainerClient).getBlobClient(FILENAME);
        verify(blockBlobClient).downloadStreamWithResponse(
            eq(outputStream), eq(blobRange), any(DownloadRetryOptions.class),
            eq(null), eq(false), eq(null), eq(null)
        );
        verifyNoInteractions(hrsCvpBlobContainerClient);
    }

    @Test
    void fetchBlobInfoShouldUseCvpClientAsDefault() {
        String hearingSource = "some-other-source";
        when(hrsCvpBlobContainerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        when(blockBlobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getBlobSize()).thenReturn(FILE_SIZE);
        when(blobProperties.getContentType()).thenReturn(CONTENT_TYPE);

        blobstoreClient.fetchBlobInfo(FILENAME, hearingSource);

        verify(blockBlobClient, times(2)).getProperties();
        verify(hrsCvpBlobContainerClient).getBlobClient(FILENAME);
        verify(hrsVhBlobContainerClient, never()).getBlobClient(anyString());
    }
}
