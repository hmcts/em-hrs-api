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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RANDOM_UUID;

@ExtendWith(MockitoExtension.class)
class SegmentServiceImplTest {
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

    @InjectMocks
    private SegmentServiceImpl underTest;

    @BeforeEach
    void setUp() {
        lenient().when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        lenient().when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);
    }

    @Test
    void testFindByRecordingId() {
        doReturn(Collections.emptyList()).when(segmentRepository).findByHearingRecordingId(RANDOM_UUID);

        final List<HearingRecordingSegment> segments = underTest.findByRecordingId(RANDOM_UUID);

        assertThat(segments).isEmpty();
        verify(segmentRepository, times(1)).findByHearingRecordingId(RANDOM_UUID);
    }

    @Test
    void testCreateAndSaveSegmentShouldMapAndSaveSuccessfully() throws IOException {
        HearingRecordingDto dto = HEARING_RECORDING_DTO;
        HearingRecording recording = HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
        String expectedMimeType = "audio/mpeg";
        when(tika.detect(any(InputStream.class))).thenReturn(expectedMimeType);

        underTest.createAndSaveSegment(recording, dto);

        ArgumentCaptor<HearingRecordingSegment> segmentCaptor = ArgumentCaptor.forClass(HearingRecordingSegment.class);
        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());

        HearingRecordingSegment capturedSegment = segmentCaptor.getValue();

        assertThat(capturedSegment.getMimeType()).isEqualTo(expectedMimeType);
        assertThat(capturedSegment.getFilename()).isEqualTo(dto.getFilename());
        assertThat(capturedSegment.getFileExtension()).isEqualTo(dto.getFilenameExtension());
        assertThat(capturedSegment.getFileSizeMb()).isEqualTo(dto.getFileSize());
        assertThat(capturedSegment.getFileMd5Checksum()).isEqualTo(dto.getCheckSum());
        assertThat(capturedSegment.getIngestionFileSourceUri()).isEqualTo(dto.getSourceBlobUrl());
        assertThat(capturedSegment.getRecordingSegment()).isEqualTo(dto.getSegment());
        assertThat(capturedSegment.getInterpreter()).isEqualTo(dto.getInterpreter());
        assertThat(capturedSegment.getHearingRecording()).isSameAs(recording);
    }

    @Test
    void testCreateAndSaveSegmentShouldPropagateExceptionWhenAzureSdkFails() {
        UncheckedIOException cause = new UncheckedIOException("Blob access error", new IOException());
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenThrow(cause);

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO)
        );

        assertThat(thrown).isSameAs(cause);
        verify(segmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreateAndSaveSegmentShouldWrapAndPropagateExceptionWhenTikaFails() throws IOException {
        IOException cause = new IOException("Tika failed to read stream");
        when(tika.detect(any(InputStream.class))).thenThrow(cause);

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO)
        );

        assertThat(thrown)
            .hasMessage("Failed to detect MIME type from blob")
            .hasCause(cause);
    }

    @Test
    void testCreateAndSaveSegmentShouldWrapAndPropagateExceptionWhenStreamCloseFails() throws IOException {
        IOException cause = new IOException("Failed to close stream");
        when(tika.detect(any(InputStream.class))).thenReturn("any-mime-type");
        doThrow(cause).when(blobInputStream).close();

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO)
        );

        assertThat(thrown).hasCause(cause);
    }

    @Test
    void testCreateAndSaveSegmentShouldHandleSuppressedExceptionWhenTikaAndCloseFail() throws IOException {
        IOException tikaException = new IOException("Tika failed to read stream");
        IOException closeException = new IOException("Failed to close stream");

        when(tika.detect(any(InputStream.class))).thenThrow(tikaException);
        doThrow(closeException).when(blobInputStream).close();

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO)
        );

        Throwable cause = thrown.getCause();
        assertThat(cause)
            .isInstanceOf(IOException.class)
            .hasMessage("Tika failed to read stream");
        assertThat(cause.getSuppressed()).hasSize(1);
        assertThat(cause.getSuppressed()[0]).isSameAs(closeException);
        verify(segmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreateAndSaveSegmentShouldPropagateConstraintViolationException() throws IOException {
        when(tika.detect(any(InputStream.class))).thenReturn("any-mime-type");
        doThrow(new ConstraintViolationException("test violation", null, null))
            .when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));

        assertThrows(
            ConstraintViolationException.class, () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO)
        );

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
    }

    @Test
    void testCreateAndSaveSegmentShouldHandleAudioMp4MimeType() throws IOException {
        final String expectedMimeType = "audio/mp4";
        when(tika.detect(any(InputStream.class))).thenReturn(expectedMimeType);

        underTest.createAndSaveSegment(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO);

        ArgumentCaptor<HearingRecordingSegment> segmentCaptor =
            ArgumentCaptor.forClass(HearingRecordingSegment.class);
        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());

        HearingRecordingSegment capturedSegment = segmentCaptor.getValue();
        assertThat(capturedSegment.getMimeType()).isEqualTo(expectedMimeType);
    }

    @Test
    void testCreateAndSaveSegmentShouldHandleVideoMp4MimeType() throws IOException {
        final String expectedMimeType = "video/mp4";
        when(tika.detect(any(InputStream.class))).thenReturn(expectedMimeType);

        underTest.createAndSaveSegment(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO);

        ArgumentCaptor<HearingRecordingSegment> segmentCaptor =
            ArgumentCaptor.forClass(HearingRecordingSegment.class);
        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());

        HearingRecordingSegment capturedSegment = segmentCaptor.getValue();
        assertThat(capturedSegment.getMimeType()).isEqualTo(expectedMimeType);
    }
}
