package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.impl.FolderServiceImpl.FilesInDatabase;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.EMPTY_FOLDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILENAME_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILENAME_2;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILENAME_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_2_JOBS_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_SEGMENTS_1_2_AND_1_JOB_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_2;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_1_NAME;


@ExtendWith(MockitoExtension.class)
class FolderServiceImplTests {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private HearingRecordingSegmentRepository segmentRepository;


    @InjectMocks
    private FolderServiceImpl folderServiceImpl;

    @Test
    @DisplayName("Test when folder is not found in the database and blobstore")
    void testShouldReturnEmptyWhenFolderIsNotFound() {
        doReturn(Optional.empty()).when(folderRepository).findByName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when folder has no HearingRecording data in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasNoHearingRecordings() {
        doReturn(Optional.of(EMPTY_FOLDER)).when(folderRepository).findByName(EMPTY_FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(EMPTY_FOLDER.getName());

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when folder has HearingRecording data with no segment in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasHearingRecordingsWithNoSegments() {
        doReturn(Optional.of(FOLDER)).when(folderRepository).findByName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when files are recorded in the database and the blobstore and no files in progress")
    void testShouldReturnCompletedFilesOnly() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);


        doReturn(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getSegments()).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles("folder-1");//TEST_FOLDER_NAME

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2, FILENAME_3));
    }

    @Test
    @DisplayName("Test when files are recorded in the database and blobstore and more files in progress")
    void testShouldReturnBothCompletedAndInProgressFiles() {


        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_AND_1_JOB_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);
        doReturn(Set.of(SEGMENT_1, SEGMENT_2, SEGMENT_3)).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2, FILENAME_3));
    }

    @Test
    @DisplayName("Test when files are recorded in the database but not in the blobstore and no files in progress")
    void testShouldExcludeWhenFileIsInDatabaseButNotInBlobstore() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_AND_1_JOB_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_3));
    }


    @Test
    @DisplayName("Test when files are recoded in the blobstore but not in the database and more files in progress")
    void testShouldReturnInProgressFilesOnly() {


        doReturn(Optional.of(FOLDER_WITH_2_JOBS_IN_PROGRESS)).when(folderRepository).findByName(TEST_FOLDER_1_NAME);

        doReturn(Set.of(SEGMENT_1, SEGMENT_2)).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2));
    }

    @Test
    @DisplayName("Should throw exception when folder not in db")
    void testShouldThrowExceptionWhenNoFolderInDB() {
        assertThatExceptionOfType(DatabaseStorageException.class).isThrownBy(() -> folderServiceImpl
            .getFolderByName("nopath"));
    }

    @Test
    @DisplayName("Should return the intersection of two sets when calling intersect")
    void testShouldReturnIntersectionOfSets() {
        final Set<String> filesetInDb = Set.of("file1.mp4", "file2.mp4", "file3.mp4");
        final Set<String> filesetInBlobstore = Set.of("file2.mp4", "file3.mp4", "file4.mp4");
        final Set<String> expectedIntersection = Set.of("file2.mp4", "file3.mp4");

        final FilesInDatabase filesInDatabase = new FilesInDatabase(filesetInDb);

        final Set<String> actualIntersection = filesInDatabase.intersect(filesetInBlobstore);

        assertThat(actualIntersection).hasSameElementsAs(expectedIntersection);
    }
}
