package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import uk.gov.hmcts.reform.em.hrs.service.Mp4MimeTypeService;

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
@DisplayName("SegmentServiceImpl")
class SegmentServiceImplTest {

    @Mock
    private HearingRecordingSegmentRepository segmentRepository;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private Mp4MimeTypeService mp4MimeTypeService;

    @InjectMocks
    private SegmentServiceImpl underTest;

    @BeforeEach
    void setUp() {
        lenient().when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    @Test
    @DisplayName("findByRecordingId should delegate to repository and return result")
    void testFindByRecordingId() {
        doReturn(Collections.emptyList()).when(segmentRepository).findByHearingRecordingId(RANDOM_UUID);

        List<HearingRecordingSegment> segments = underTest.findByRecordingId(RANDOM_UUID);

        assertThat(segments).isEmpty();
        verify(segmentRepository, times(1)).findByHearingRecordingId(RANDOM_UUID);
    }

    @Test
    @DisplayName("createAndSaveSegment should map DTO to entity and persist with detected MIME type")
    void testCreateAndSaveSegmentShouldMapAndSaveSuccessfully() {
        HearingRecordingDto dto = HEARING_RECORDING_DTO;
        HearingRecording recording = HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
        String expectedMimeType = "audio/mpeg";

        when(mp4MimeTypeService.getMimeType(blobClient)).thenReturn(expectedMimeType);

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

        verify(blobContainerClient).getBlobClient(dto.getFilename());
        verify(mp4MimeTypeService).getMimeType(blobClient);
    }

    @Test
    @DisplayName("createAndSaveSegment should propagate repository ConstraintViolationException")
    void testCreateAndSaveSegmentShouldPropagateConstraintViolationException() {
        when(mp4MimeTypeService.getMimeType(blobClient)).thenReturn("any-mime-type");
        doThrow(new ConstraintViolationException("test violation", null, null))
            .when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));

        assertThrows(
            ConstraintViolationException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO
            )
        );

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
    }

    @Test
    @DisplayName("createAndSaveSegment should handle audio/mp4 MIME type")
    void testCreateAndSaveSegmentShouldHandleAudioMp4MimeType() {
        String expectedMimeType = "audio/mp4";
        when(mp4MimeTypeService.getMimeType(blobClient)).thenReturn(expectedMimeType);

        underTest.createAndSaveSegment(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO);

        ArgumentCaptor<HearingRecordingSegment> segmentCaptor = ArgumentCaptor.forClass(HearingRecordingSegment.class);
        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());

        HearingRecordingSegment capturedSegment = segmentCaptor.getValue();
        assertThat(capturedSegment.getMimeType()).isEqualTo(expectedMimeType);
    }

    @Test
    @DisplayName("createAndSaveSegment should handle video/mp4 MIME type")
    void testCreateAndSaveSegmentShouldHandleVideoMp4MimeType() {
        String expectedMimeType = "video/mp4";
        when(mp4MimeTypeService.getMimeType(blobClient)).thenReturn(expectedMimeType);

        underTest.createAndSaveSegment(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO);

        ArgumentCaptor<HearingRecordingSegment> segmentCaptor = ArgumentCaptor.forClass(HearingRecordingSegment.class);
        verify(segmentRepository).saveAndFlush(segmentCaptor.capture());

        HearingRecordingSegment capturedSegment = segmentCaptor.getValue();
        assertThat(capturedSegment.getMimeType()).isEqualTo(expectedMimeType);
    }

    @Test
    @DisplayName("createAndSaveSegment should not save when MIME detection throws at infrastructure level")
    void testCreateAndSaveSegmentShouldNotSaveWhenMimeDetectionFails() {
        RuntimeException cause = new RuntimeException("MIME detection failed");
        when(mp4MimeTypeService.getMimeType(blobClient)).thenThrow(cause);

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> underTest.createAndSaveSegment(
                HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3, HEARING_RECORDING_DTO
            )
        );

        assertThat(thrown).isSameAs(cause);
        verify(segmentRepository, never()).saveAndFlush(any());
    }
}
