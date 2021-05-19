package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.EMPTY_FOLDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILE_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILE_2;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILE_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_JOBS_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_SEGMENT;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_SEGMENT_AND_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_NAME;

@ExtendWith(MockitoExtension.class)
class FolderServiceImplTests {
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private JobInProgressRepository jobInProgressRepository;
    @Mock
    private HearingRecordingStorage hearingRecordingStorage;

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;


    @InjectMocks
    private FolderServiceImpl underTest;

    @BeforeEach
    void prepare() {
        lenient().doNothing().when(jobInProgressRepository).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when folder is not found in the database and blobstore")
    void testShouldReturnEmptyWhenFolderIsNotFound() {
        doReturn(Optional.empty()).when(folderRepository).findByName(TEST_FOLDER_NAME);

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when folder has no HearingRecording data in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasNoHearingRecordings() {
        doReturn(Optional.of(EMPTY_FOLDER)).when(folderRepository).findByName(EMPTY_FOLDER.getName());
        doReturn(Collections.emptySet()).when(hearingRecordingStorage).findByFolder(EMPTY_FOLDER.getName());

        Set<String> actualFilenames = underTest.getStoredFiles(EMPTY_FOLDER.getName());

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when folder has HearingRecording data with no segment in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasHearingRecordingsWithNoSegments() {
        doReturn(Optional.of(FOLDER)).when(folderRepository).findByName(TEST_FOLDER_NAME);
        doReturn(Collections.emptySet()).when(hearingRecordingStorage).findByFolder(FOLDER.getName());

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when files are recorded in the database and the blobstore and no files in progress")
    void testShouldReturnCompletedFilesOnly() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENT)).when(folderRepository).findByName(TEST_FOLDER_NAME);
        doReturn(Set.of(FILE_1, FILE_2, FILE_3)).when(hearingRecordingStorage).findByFolder(FOLDER.getName());

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILE_1, FILE_2, FILE_3));
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when files are recorded in the database and blobstore and more files in progress")
    void testShouldReturnBothCompletedAndInProgressFiles() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENT_AND_IN_PROGRESS)).when(folderRepository).findByName(TEST_FOLDER_NAME);
        doReturn(Set.of(FILE_1, FILE_2)).when(hearingRecordingStorage).findByFolder(FOLDER.getName());

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILE_1, FILE_2, FILE_3));
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test when files are recorded in the database but not in the blobstore and no files in progress")
    void testShouldExcludeWhenFileIsInDatabaseButNotInBlobstore() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENT_AND_IN_PROGRESS)).when(folderRepository).findByName(TEST_FOLDER_NAME);
        doReturn(Collections.emptySet()).when(hearingRecordingStorage).findByFolder(FOLDER.getName());

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILE_3));
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }


    @Test
    @DisplayName("Test when files are recoded in the blobstore but not in the database and more files in progress")
    void testShouldReturnInProgressFilesOnly() {
        doReturn(Optional.of(FOLDER_WITH_JOBS_IN_PROGRESS)).when(folderRepository).findByName(TEST_FOLDER_NAME);
        doReturn(Set.of(FILE_3)).when(hearingRecordingStorage).findByFolder(TEST_FOLDER_NAME);

        Set<String> actualFilenames = underTest.getStoredFiles(TEST_FOLDER_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILE_1, FILE_2));
        verify(jobInProgressRepository, times(1)).deleteByCreatedOnLessThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should throw exception when folder not in db")
    void testShouldThrowExceptionWhenNoFolderInDB() {
        assertThatExceptionOfType(DatabaseStorageException.class).isThrownBy(() -> underTest.getFolderByName("nopath"));
    }
}
