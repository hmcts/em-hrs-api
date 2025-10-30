package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.BlobStorageDeleteService;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_1;

@ExtendWith(MockitoExtension.class)
class HearingRecordingServiceImplTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
    @Mock
    private BlobStorageDeleteService blobStorageDeleteService;
    @Mock
    private HearingRecordingSegmentRepository hearingRecordingSegmentRepository;
    @Mock
    private ShareesRepository shareesRepository;
    @Mock
    private HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;
    @Mock
    private HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;
    @Mock
    private ShareesAuditEntryRepository shareesAuditEntryRepository;
    @Mock
    private FolderService folderService;

    @InjectMocks
    private HearingRecordingServiceImpl recordingService;

    @Test
    void testFindHearingRecordingShouldReturnRecordingWhenFound() {
        HearingRecording expectedRecording = new HearingRecording();
        doReturn(Optional.of(expectedRecording)).when(hearingRecordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, HEARING_RECORDING_DTO.getFolder());

        Optional<HearingRecording> result = recordingService.findHearingRecording(HEARING_RECORDING_DTO);

        assertThat(result).isPresent().contains(expectedRecording);
        verify(hearingRecordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, HEARING_RECORDING_DTO.getFolder());
    }

    @Test
    void testFindHearingRecordingShouldReturnEmptyWhenNotFound() {
        doReturn(Optional.empty()).when(hearingRecordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, HEARING_RECORDING_DTO.getFolder());

        Optional<HearingRecording> result = recordingService.findHearingRecording(HEARING_RECORDING_DTO);

        assertThat(result).isEmpty();
        verify(hearingRecordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, HEARING_RECORDING_DTO.getFolder());
    }

    @Test
    void testCreateHearingRecordingShouldBuildAndSaveEntity() {
        Folder folder = new Folder();
        folder.setName(HEARING_RECORDING_DTO.getFolder());
        doReturn(folder).when(folderService).getFolderByName(HEARING_RECORDING_DTO.getFolder());

        recordingService.createHearingRecording(HEARING_RECORDING_DTO);

        ArgumentCaptor<HearingRecording> captor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(hearingRecordingRepository).saveAndFlush(captor.capture());
        HearingRecording captured = captor.getValue();

        assertThat(captured.getFolder()).isEqualTo(folder);
        assertThat(captured.getCaseRef()).isEqualTo(HEARING_RECORDING_DTO.getCaseRef());
        assertThat(captured.getRecordingRef()).isEqualTo(HEARING_RECORDING_DTO.getRecordingRef());
        assertThat(captured.getHearingLocationCode()).isEqualTo(HEARING_RECORDING_DTO.getCourtLocationCode());
        assertThat(captured.getHearingRoomRef()).isEqualTo(HEARING_RECORDING_DTO.getHearingRoomRef());
        assertThat(captured.getServiceCode()).isEqualTo(HEARING_RECORDING_DTO.getServiceCode());
        assertThat(captured.getJurisdictionCode()).isEqualTo(HEARING_RECORDING_DTO.getJurisdictionCode());
        assertThat(captured.getHearingSource()).isEqualTo(HEARING_RECORDING_DTO.getRecordingSource().name());
    }

    @Test
    void testCreateHearingRecordingShouldThrowCcdUploadExceptionOnConstraintViolation() {
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(HEARING_RECORDING_DTO.getFolder());
        doThrow(new ConstraintViolationException("test violation", null, null))
            .when(hearingRecordingRepository).saveAndFlush(any(HearingRecording.class));

        CcdUploadException exception = assertThrows(CcdUploadException.class, () ->
            recordingService.createHearingRecording(HEARING_RECORDING_DTO)
        );

        assertThat(exception.getMessage())
            .isEqualTo("Hearing Recording already exists. Likely race condition from another server");
    }

    @Test
    void testCreateHearingRecordingShouldThrowCcdUploadExceptionOnGenericException() {
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(HEARING_RECORDING_DTO.getFolder());
        doThrow(new RuntimeException("Generic DB error"))
            .when(hearingRecordingRepository).saveAndFlush(any(HearingRecording.class));

        CcdUploadException exception = assertThrows(CcdUploadException.class, () ->
            recordingService.createHearingRecording(HEARING_RECORDING_DTO)
        );

        assertThat(exception.getMessage()).isEqualTo("Unhandled Exception trying to persist case");
    }

    @Test
    void testUpdateCcdCaseIdShouldSetIdAndSave() {
        HearingRecording recording = new HearingRecording();
        assertThat(recording.getCcdCaseId()).isNull();

        recordingService.updateCcdCaseId(recording, CCD_CASE_ID);

        ArgumentCaptor<HearingRecording> captor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(hearingRecordingRepository).saveAndFlush(captor.capture());

        HearingRecording captured = captor.getValue();
        assertThat(captured.getCcdCaseId()).isEqualTo(CCD_CASE_ID);
        assertThat(captured).isSameAs(recording);
    }

    @Test
    void deleteCaseHearingRecordingsDeletesAllRelatedDataSuccessfully() {
        List<Long> ccdCaseIds = List.of(1L, 2L);
        UUID hearingRecordingId = UUID.randomUUID();
        List<HearingRecordingDeletionDto> hearingRecordingDtos = List.of(
            new HearingRecordingDeletionDto(hearingRecordingId, null, null, "CVP", null)
        );
        List<HearingRecordingDeletionDto> segmentDtos = List.of(
            new HearingRecordingDeletionDto(hearingRecordingId, UUID.randomUUID(), null, "CVP", "file1.mp4"),
            new HearingRecordingDeletionDto(hearingRecordingId, UUID.randomUUID(), null, "VH", "file2.mp4")
        );

        doReturn(hearingRecordingDtos).when(hearingRecordingRepository)
            .findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds);
        doReturn(segmentDtos).when(hearingRecordingSegmentRepository)
            .findFilenamesByHearingRecordingId(any(UUID.class));

        recordingService.deleteCaseHearingRecordings(ccdCaseIds);

        verify(blobStorageDeleteService, times(2)).deleteBlob(any(String.class), any(HearingSource.class));
        verify(hearingRecordingSegmentAuditEntryRepository, times(2))
            .deleteByHearingRecordingSegmentId(any(UUID.class));
        verify(hearingRecordingSegmentRepository, times(2)).deleteById(any(UUID.class));
        verify(shareesAuditEntryRepository, times(1)).deleteByHearingRecordingShareeIds(anyList());
        verify(shareesRepository).deleteByHearingRecordingIds(anyList());
        verify(hearingRecordingAuditEntryRepository).deleteByHearingRecordingIds(anyList());
        verify(hearingRecordingRepository).deleteByHearingRecordingIds(anyList());
    }

    @Test
    void deleteCaseHearingRecordingsHandlesEmptyCcdCaseIdsGracefully() {
        List<Long> ccdCaseIds = List.of();

        recordingService.deleteCaseHearingRecordings(ccdCaseIds);

        verifyNoInteractions(blobStorageDeleteService);
        verifyNoInteractions(hearingRecordingSegmentAuditEntryRepository);
        verifyNoInteractions(hearingRecordingSegmentRepository);
        verifyNoInteractions(shareesAuditEntryRepository);
        verifyNoInteractions(shareesRepository);
        verifyNoInteractions(hearingRecordingAuditEntryRepository);
    }

    @Test
    void deleteCaseHearingRecordingsHandlesNullCcdCaseIdsGracefully() {
        recordingService.deleteCaseHearingRecordings(null);

        verifyNoInteractions(blobStorageDeleteService);
        verifyNoInteractions(hearingRecordingSegmentAuditEntryRepository);
        verifyNoInteractions(hearingRecordingSegmentRepository);
        verifyNoInteractions(shareesAuditEntryRepository);
        verifyNoInteractions(shareesRepository);
        verifyNoInteractions(hearingRecordingAuditEntryRepository);
    }

    @Test
    void deleteCaseHearingRecordingsLogsErrorOnException() {
        List<Long> ccdCaseIds = List.of(1L);
        doThrow(RuntimeException.class).when(hearingRecordingRepository)
            .findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds);

        recordingService.deleteCaseHearingRecordings(ccdCaseIds);

        verifyNoInteractions(blobStorageDeleteService);
        verifyNoInteractions(hearingRecordingSegmentAuditEntryRepository);
        verifyNoInteractions(hearingRecordingSegmentRepository);
        verifyNoInteractions(shareesAuditEntryRepository);
        verifyNoInteractions(shareesRepository);
        verifyNoInteractions(hearingRecordingAuditEntryRepository);
    }

    @Test
    void findCcdCaseIdByFilenameReturnsCcdCaseIdWhenFilenameExists() {
        String filename = "file1.mp4";
        Long expectedCcdCaseId = 12345L;
        doReturn(expectedCcdCaseId).when(hearingRecordingRepository).findCcdCaseIdByFilename(filename);

        Long actualCcdCaseId = recordingService.findCcdCaseIdByFilename(filename);

        assertEquals(expectedCcdCaseId, actualCcdCaseId);
    }

    @Test
    void findCcdCaseIdByFilenameReturnsNullWhenFilenameDoesNotExist() {
        String filename = "nonexistent.mp4";
        doReturn(null).when(hearingRecordingRepository).findCcdCaseIdByFilename(filename);

        Long actualCcdCaseId = recordingService.findCcdCaseIdByFilename(filename);

        assertNull(actualCcdCaseId);
    }

    @Test
    void findCcdCaseIdByFilenameThrowsExceptionWhenRepositoryFails() {
        String filename = "file1.mp4";
        doThrow(RuntimeException.class).when(hearingRecordingRepository).findCcdCaseIdByFilename(filename);
        assertThrows(RuntimeException.class, () -> recordingService.findCcdCaseIdByFilename(filename));
    }
}
