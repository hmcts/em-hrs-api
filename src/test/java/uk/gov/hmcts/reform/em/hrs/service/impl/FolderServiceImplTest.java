package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.impl.FolderServiceImpl.FilesInDatabase;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceImplTest {

    private static final String TEST_FOLDER_NAME = "folder-123";
    private static final String SEGMENT_FILE_1 = "segment-1.mp4";
    private static final String SEGMENT_FILE_2 = "segment-2.mp4";
    private static final String JOB_FILE_1 = "job-1.mp4";

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private HearingRecordingSegmentRepository hearingRecordingSegmentRepository;

    @InjectMocks
    private FolderServiceImpl folderService;

    @Nested
    @DisplayName("getStoredFiles")
    class GetStoredFiles {

        @Test
        void getStoredFilesWhenFolderDoesNotExistShouldCreateNewFolderAndReturnEmptySet() {
            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.empty());

            Set<String> result = folderService.getStoredFiles(TEST_FOLDER_NAME);

            assertThat(result).isEmpty();

            ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
            verify(folderRepository).save(folderCaptor.capture());

            Folder savedFolder = folderCaptor.getValue();
            assertThat(savedFolder.getName()).isEqualTo(TEST_FOLDER_NAME);
            verify(hearingRecordingSegmentRepository, never()).findByHearingRecordingFolderName(any());
        }

        @Test
        void getStoredFilesWhenFolderExistsWithSegmentsAndJobsShouldReturnCombinedSet() {
            JobInProgress job = new JobInProgress();
            job.setFilename(JOB_FILE_1);

            Folder existingFolder = Folder.builder()
                .name(TEST_FOLDER_NAME)
                .jobsInProgress(List.of(job))
                .build();

            HearingRecordingSegment segment1 = new HearingRecordingSegment();
            segment1.setFilename(SEGMENT_FILE_1);
            HearingRecordingSegment segment2 = new HearingRecordingSegment();
            segment2.setFilename(SEGMENT_FILE_2);

            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.of(existingFolder));
            when(hearingRecordingSegmentRepository.findByHearingRecordingFolderName(TEST_FOLDER_NAME))
                .thenReturn(Set.of(segment1, segment2));

            Set<String> result = folderService.getStoredFiles(TEST_FOLDER_NAME);

            assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(SEGMENT_FILE_1, SEGMENT_FILE_2, JOB_FILE_1);
        }

        @Test
        void getStoredFilesWhenFolderExistsButIsEmptyShouldReturnEmptySet() {
            Folder existingFolder = Folder.builder()
                .name(TEST_FOLDER_NAME)
                .jobsInProgress(Collections.emptyList())
                .build();

            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.of(existingFolder));
            when(hearingRecordingSegmentRepository.findByHearingRecordingFolderName(TEST_FOLDER_NAME))
                .thenReturn(Collections.emptySet());

            Set<String> result = folderService.getStoredFiles(TEST_FOLDER_NAME);

            assertThat(result).isEmpty();
        }

        @Test
        void getStoredFilesWhenFilesOverlapBetweenSegmentsAndJobsShouldReturnUniqueSet() {
            String duplicateFile = "duplicate.mp4";

            JobInProgress job = new JobInProgress();
            job.setFilename(duplicateFile);

            Folder existingFolder = Folder.builder()
                .name(TEST_FOLDER_NAME)
                .jobsInProgress(List.of(job))
                .build();

            HearingRecordingSegment segment = new HearingRecordingSegment();
            segment.setFilename(duplicateFile);

            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.of(existingFolder));
            when(hearingRecordingSegmentRepository.findByHearingRecordingFolderName(TEST_FOLDER_NAME))
                .thenReturn(Set.of(segment));

            Set<String> result = folderService.getStoredFiles(TEST_FOLDER_NAME);

            assertThat(result)
                .hasSize(1)
                .containsExactly(duplicateFile);
        }
    }

    @Nested
    @DisplayName("getFolderByName")
    class GetFolderByName {

        @Test
        void getFolderByNameWhenFolderExistsShouldReturnFolder() {
            UUID folderId = UUID.randomUUID();
            Folder folder = Folder.builder().id(folderId).name(TEST_FOLDER_NAME).build();
            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.of(folder));

            Folder result = folderService.getFolderByName(TEST_FOLDER_NAME);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(folderId);
            assertThat(result.getName()).isEqualTo(TEST_FOLDER_NAME);
        }

        @Test
        void getFolderByNameWhenNotFoundShouldThrowDatabaseStorageException() {
            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> folderService.getFolderByName(TEST_FOLDER_NAME))
                .isInstanceOf(DatabaseStorageException.class)
                .hasMessage("Folders must explicitly exist, based on GET /folders/(foldername) creating them");
        }
    }

    @Nested
    @DisplayName("FilesInDatabase")
    class FilesInDatabaseTest {

        @Test
        void intersectShouldReturnIntersectionOfTwoSets() {
            Set<String> databaseFiles = Set.of("A", "B", "C");
            FilesInDatabase filesInDatabase = new FilesInDatabase(databaseFiles);
            Set<String> blobstoreFiles = Set.of("B", "C", "D");

            Set<String> result = filesInDatabase.intersect(blobstoreFiles);

            assertThat(result)
                .containsExactlyInAnyOrder("B", "C")
                .doesNotContain("A", "D");
        }

        @Test
        void intersectShouldReturnEmptyIfNoOverlapExists() {
            FilesInDatabase filesInDatabase = new FilesInDatabase(Set.of("A"));

            Set<String> result = filesInDatabase.intersect(Set.of("B"));

            assertThat(result).isEmpty();
        }
    }
}
