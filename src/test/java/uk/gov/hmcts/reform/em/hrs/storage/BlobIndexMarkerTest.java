package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class BlobIndexMarkerTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @InjectMocks
    private BlobIndexMarker underTest;

    @Test
    void markIndexAsProcessed() {
        String blobName = "test/dc/a.txt";
        var blobClient = mock(BlobClient.class);
        doReturn(blobClient).when(blobContainerClient).getBlobClient(blobName);
        var tags = new HashMap<String, String>();
        tags.put("leased", "true");
        doReturn(tags).when(blobClient).getTags();

        var result = underTest.setProcessed(blobName);
        assertThat(result).isTrue();
        verify(blobClient, times(1))
            .setTags(Map.of("leased", "true", "processed", "true"));
    }

    @Test
    void markProcessedThrowsError() {
        String blobName = "test/dc/a.txt";
        doThrow(IllegalArgumentException.class).when(blobContainerClient).getBlobClient(blobName);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> underTest.setProcessed(blobName));
    }


}
