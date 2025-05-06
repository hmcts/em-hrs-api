package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.BlobStorageDeleteService;

import java.util.List;
import java.util.UUID;

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
    @InjectMocks
    private HearingRecordingServiceImpl recordingService;

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

        verify(blobStorageDeleteService,times(2)).deleteBlob(any(String.class), any(HearingSource.class));
        verify(hearingRecordingSegmentAuditEntryRepository,times(2))
                .deleteByHearingRecordingSegmentId(any(UUID.class));
        verify(hearingRecordingSegmentRepository, times(2)).deleteByHearingRecordingId(any(UUID.class));
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
