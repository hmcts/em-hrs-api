package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
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

import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingRecordingServiceImplTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
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
    @Mock
    private BlobStorageDeleteService blobStorageDeleteService;

    @InjectMocks
    private HearingRecordingServiceImpl hearingRecordingService;

    @Nested
    @DisplayName("deleteCaseHearingRecordings")
    class DeleteCaseHearingRecordings {

        @Test
        void deleteCaseHearingRecordingsWhenNoRecordingsFoundShouldDoNothing() {
            List<Long> ccdCaseIds = List.of(12345L);
            when(hearingRecordingRepository.findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds))
                .thenReturn(Collections.emptyList());

            hearingRecordingService.deleteCaseHearingRecordings(ccdCaseIds);

            verify(hearingRecordingSegmentRepository, never()).findFilenamesByHearingRecordingId(any());
            verifyNoInteractions(blobStorageDeleteService);
            verifyNoInteractions(shareesRepository);
        }

        @Test
        void deleteCaseHearingRecordingsWhenRecordingsFoundShouldDeleteAllAssociatedData() {
            List<Long> ccdCaseIds = List.of(12345L);
            UUID recordingId = UUID.randomUUID();
            UUID segmentId = UUID.randomUUID();
            UUID shareeId = UUID.randomUUID();
            String filename = "recording.mp4";
            String source = "CVP";

            HearingRecordingDeletionDto recordingDto = new HearingRecordingDeletionDto(
                recordingId, null, null, source, null
            );

            HearingRecordingDeletionDto segmentDto = new HearingRecordingDeletionDto(
                recordingId, segmentId, null, source, filename
            );

            when(hearingRecordingRepository.findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds))
                .thenReturn(List.of(recordingDto));

            when(hearingRecordingSegmentRepository.findFilenamesByHearingRecordingId(recordingId))
                .thenReturn(List.of(segmentDto));

            when(shareesRepository.findAllByHearingRecordingIds(List.of(recordingId)))
                .thenReturn(List.of(shareeId));

            hearingRecordingService.deleteCaseHearingRecordings(ccdCaseIds);

            verify(blobStorageDeleteService).deleteBlob(filename, HearingSource.CVP);
            verify(hearingRecordingSegmentAuditEntryRepository).deleteByHearingRecordingSegmentId(segmentId);
            verify(hearingRecordingSegmentRepository).deleteById(segmentId);
            verify(shareesAuditEntryRepository).deleteByHearingRecordingShareeIds(List.of(shareeId));
            verify(shareesRepository).deleteByHearingRecordingIds(List.of(recordingId));
            verify(hearingRecordingAuditEntryRepository).deleteByHearingRecordingIds(List.of(recordingId));
            verify(hearingRecordingRepository).deleteByHearingRecordingIds(List.of(recordingId));
        }

        @Test
        void deleteCaseHearingRecordingsWhenExceptionOccursShouldCatchAndLogWithoutThrowing() {
            List<Long> ccdCaseIds = List.of(12345L);
            when(hearingRecordingRepository.findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds))
                .thenThrow(new RuntimeException("Database down"));

            hearingRecordingService.deleteCaseHearingRecordings(ccdCaseIds);

            verifyNoInteractions(blobStorageDeleteService);
        }
    }

    @Nested
    @DisplayName("findHearingRecording")
    class FindHearingRecording {

        @Test
        void findHearingRecordingShouldReturnRecordingWhenFound() {
            HearingRecordingDto dto = HearingRecordingDto.builder()
                .recordingRef("ref")
                .folder("folder")
                .build();

            HearingRecording expectedRecording = new HearingRecording();
            when(hearingRecordingRepository.findByRecordingRefAndFolderName("ref", "folder"))
                .thenReturn(Optional.of(expectedRecording));

            Optional<HearingRecording> result = hearingRecordingService.findHearingRecording(dto);

            assertThat(result).isPresent().contains(expectedRecording);
        }

        @Test
        void findHearingRecordingShouldReturnEmptyWhenNotFound() {
            HearingRecordingDto dto = HearingRecordingDto.builder()
                .recordingRef("ref")
                .folder("folder")
                .build();

            when(hearingRecordingRepository.findByRecordingRefAndFolderName("ref", "folder"))
                .thenReturn(Optional.empty());

            Optional<HearingRecording> result = hearingRecordingService.findHearingRecording(dto);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createHearingRecording")
    class CreateHearingRecording {

        private HearingRecordingDto validDto;

        @Test
        void createHearingRecordingWhenSuccessfulShouldReturnSavedRecording() {
            validDto = HearingRecordingDto.builder()
                .folder("folder")
                .recordingRef("ref")
                .caseRef("caseRef")
                .courtLocationCode("loc")
                .hearingRoomRef("room")
                .recordingSource(HearingSource.CVP)
                .jurisdictionCode("jurisdiction")
                .serviceCode("service")
                .build();

            Folder folder = new Folder();
            when(folderService.getFolderByName("folder")).thenReturn(folder);

            HearingRecording savedRecording = new HearingRecording();
            when(hearingRecordingRepository.saveAndFlush(any(HearingRecording.class))).thenReturn(savedRecording);

            HearingRecording result = hearingRecordingService.createHearingRecording(validDto);

            assertThat(result).isEqualTo(savedRecording);
            verify(hearingRecordingRepository).saveAndFlush(any(HearingRecording.class));
        }

        @Test
        void createHearingRecordingWhenConstraintViolationOccursShouldThrowCcdUploadException() {
            validDto = HearingRecordingDto.builder()
                .folder("folder")
                .recordingSource(HearingSource.CVP)
                .build();

            when(folderService.getFolderByName("folder")).thenReturn(new Folder());

            ConstraintViolationException constraintException =
                new ConstraintViolationException("duplicate", new SQLException(), "constraint");

            when(hearingRecordingRepository.saveAndFlush(any(HearingRecording.class)))
                .thenThrow(constraintException);

            assertThatThrownBy(() -> hearingRecordingService.createHearingRecording(validDto))
                .isInstanceOf(CcdUploadException.class)
                .hasMessage("Hearing Recording already exists. Likely race condition from another server");
        }

        @Test
        void createHearingRecordingWhenGenericExceptionOccursShouldThrowCcdUploadException() {
            validDto = HearingRecordingDto.builder()
                .folder("folder")
                .recordingSource(HearingSource.CVP)
                .build();

            when(folderService.getFolderByName("folder")).thenReturn(new Folder());

            when(hearingRecordingRepository.saveAndFlush(any(HearingRecording.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            assertThatThrownBy(() -> hearingRecordingService.createHearingRecording(validDto))
                .isInstanceOf(CcdUploadException.class)
                .hasMessage("Unhandled Exception trying to persist case");
        }
    }

    @Nested
    @DisplayName("updateCcdCaseId")
    class UpdateCcdCaseId {

        @Test
        void updateCcdCaseIdShouldUpdateIdAndSave() {
            HearingRecording recording = new HearingRecording();
            Long ccdCaseId = 999L;

            when(hearingRecordingRepository.saveAndFlush(recording)).thenReturn(recording);

            HearingRecording result = hearingRecordingService.updateCcdCaseId(recording, ccdCaseId);

            assertThat(result.getCcdCaseId()).isEqualTo(ccdCaseId);
            verify(hearingRecordingRepository).saveAndFlush(recording);
        }
    }

    @Nested
    @DisplayName("findCcdCaseIdByFilename")
    class FindCcdCaseIdByFilename {

        @Test
        void findCcdCaseIdByFilenameShouldReturnId() {
            String filename = "file.mp4";
            Long expectedId = 888L;
            when(hearingRecordingRepository.findCcdCaseIdByFilename(filename)).thenReturn(expectedId);

            Long result = hearingRecordingService.findCcdCaseIdByFilename(filename);

            assertThat(result).isEqualTo(expectedId);
        }
    }

    @Test
    void testGetRecordingsForTtlUpdateShouldReturnDtoList() {
        int limit = 50;
        List<HearingRecordingTtlMigrationDTO> expectedDtos = List.of();

        PageRequest expectedPageRequest = PageRequest.of(0, limit);

        doReturn(expectedDtos).when(hearingRecordingRepository)
            .findRecordsForTtlUpdate(expectedPageRequest);

        List<HearingRecordingTtlMigrationDTO> result = recordingService.getRecordingsForTtlUpdate(limit);

        assertThat(result).isSameAs(expectedDtos);
        verify(hearingRecordingRepository).findRecordsForTtlUpdate(expectedPageRequest);
    }

    @Test
    void testUpdateTtlShouldInvokeRepositoryUpdate() {
        UUID recordingId = UUID.randomUUID();
        LocalDate ttl = LocalDate.now();

        recordingService.updateTtl(recordingId, ttl);

        verify(hearingRecordingRepository).updateTtlById(recordingId, ttl);
    }
}
