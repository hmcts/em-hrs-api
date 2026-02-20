package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.apache.tika.Tika;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegmentServiceImplTest {

    private static final String TEST_FILENAME = "test-recording.mp3";
    private static final String BLOB_URL = "https://blob.storage/container/file.mp3";

    @Mock
    private HearingRecordingSegmentRepository segmentRepository;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobInputStream blobInputStream;
    @Mock
    private Tika tika;

    @Captor
    private ArgumentCaptor<HearingRecordingSegment> segmentCaptor;

    @InjectMocks
    private SegmentServiceImpl segmentService;

    private HearingRecording hearingRecording;
    private HearingRecordingDto recordingDto;

    @BeforeEach
    void setUp() {
        hearingRecording = new HearingRecording();
        hearingRecording.setId(UUID.randomUUID());

        recordingDto = HearingRecordingDto.builder()
            .filename(TEST_FILENAME)
            .filenameExtension("mp3")
            .fileSize(100L)
            .checkSum("abc123def456")
            .sourceBlobUrl(BLOB_URL)
            .segment(1)
            .interpreter("0")
            .build();
    }

    @Test
    void findByRecordingIdShouldReturnSegmentsWhenFound() {
        UUID recordingId = UUID.randomUUID();
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentRepository.findByHearingRecordingId(recordingId)).thenReturn(List.of(segment));

        List<HearingRecordingSegment> result = segmentService.findByRecordingId(recordingId);

        assertThat(result).hasSize(1).contains(segment);
    }

    @Test
    void findByRecordingIdShouldReturnEmptyListWhenNotFound() {
        UUID recordingId = UUID.randomUUID();
        when(segmentRepository.findByHearingRecordingId(recordingId)).thenReturn(Collections.emptyList());

        List<HearingRecordingSegment> result = segmentService.findByRecordingId(recordingId);

        assertThat(result).isEmpty();
    }

    @Test
    void createAndSaveSegmentShouldMapAndSaveSuccessfully() throws IOException {
        String expectedMimeType = "audio/mpeg";

        when(blobContainerClient.getBlobClient(TEST_FILENAME)).thenReturn(blobClient);
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);
        when(tika.detect(any(InputStream.class))).thenReturn(expectedMimeType);

        segmentService.createAndSaveSegment(hearingRecording, recordingDto);

        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());
        HearingRecordingSegment savedSegment = segmentCaptor.getValue();

        assertThat(savedSegment.getMimeType()).isEqualTo(expectedMimeType);
        assertThat(savedSegment.getFilename()).isEqualTo(TEST_FILENAME);
        assertThat(savedSegment.getFileExtension()).isEqualTo("mp3");
        assertThat(savedSegment.getFileSizeMb()).isEqualTo(100L);
        assertThat(savedSegment.getFileMd5Checksum()).isEqualTo("abc123def456");
        assertThat(savedSegment.getIngestionFileSourceUri()).isEqualTo(BLOB_URL);
        assertThat(savedSegment.getHearingRecording()).isEqualTo(hearingRecording);

        verify(blobInputStream).close();
    }

    @Test
    void createAndSaveSegmentShouldPropagateExceptionWhenAzureSdkFails() {
        when(blobContainerClient.getBlobClient(TEST_FILENAME)).thenReturn(blobClient);

        UncheckedIOException cause = new UncheckedIOException(new IOException("Connection reset"));
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenThrow(cause);

        assertThatThrownBy(() -> segmentService.createAndSaveSegment(hearingRecording, recordingDto))
            .isSameAs(cause);
    }

    @Test
    void createAndSaveSegmentShouldWrapAndPropagateExceptionWhenTikaFails() throws IOException {
        when(blobContainerClient.getBlobClient(TEST_FILENAME)).thenReturn(blobClient);
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);

        IOException tikaException = new IOException("Tika failed to read stream");
        when(tika.detect(any(InputStream.class))).thenThrow(tikaException);

        assertThatThrownBy(() -> segmentService.createAndSaveSegment(hearingRecording, recordingDto))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("Failed to detect MIME type from blob")
            .hasCause(tikaException);
    }

    @Test
    void createAndSaveSegmentShouldPropagateConstraintViolationException() throws IOException {
        when(blobContainerClient.getBlobClient(TEST_FILENAME)).thenReturn(blobClient);
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);
        when(tika.detect(any(InputStream.class))).thenReturn("audio/mp4");

        doThrow(new ConstraintViolationException("Duplicate segment", null, "constraint_name"))
            .when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));

        assertThatThrownBy(() -> segmentService.createAndSaveSegment(hearingRecording, recordingDto))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void createAndSaveSegmentShouldHandleSuppressedExceptionWhenTikaAndCloseFail() throws IOException {
        when(blobContainerClient.getBlobClient(TEST_FILENAME)).thenReturn(blobClient);
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);

        IOException tikaException = new IOException("Primary error");
        IOException closeException = new IOException("Close error");

        when(tika.detect(any(InputStream.class))).thenThrow(tikaException);
        doThrow(closeException).when(blobInputStream).close();

        assertThatThrownBy(() -> segmentService.createAndSaveSegment(hearingRecording, recordingDto))
            .isInstanceOf(UncheckedIOException.class)
            .satisfies(exception -> {
                Throwable cause = exception.getCause();
                assertThat(cause).isEqualTo(tikaException);
                assertThat(cause.getSuppressed()).contains(closeException);
            });
    }
}
