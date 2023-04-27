package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

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
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RESOURCE;
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

    @Mock
    private HearingRecordingStorage blobStorage;

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
    void testShouldReturnEmptyWhenFolderIsNotFoundByHearingResource() {
        doReturn(Optional.empty()).when(folderRepository)
            .findByNameAndHearingSource(TEST_FOLDER_1_NAME, HEARING_RESOURCE);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME, HEARING_RESOURCE);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when folder has no HearingRecording data in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasNoHearingRecordings() {
        doReturn(Optional.of(EMPTY_FOLDER)).when(folderRepository).findByName(EMPTY_FOLDER.getName());
        doReturn(Collections.emptySet()).when(blobStorage).findByFolderName(EMPTY_FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(EMPTY_FOLDER.getName());

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    void testShouldReturnEmptyWhenFolderHasNoHearingRecordingsByHearingResource() {
        doReturn(Optional.of(EMPTY_FOLDER)).when(folderRepository)
            .findByNameAndHearingSource(EMPTY_FOLDER.getName(),HEARING_RESOURCE);
        doReturn(Collections.emptySet()).when(blobStorage).findByFolderName(EMPTY_FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(EMPTY_FOLDER.getName(), HEARING_RESOURCE);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when folder has HearingRecording data with no segment in the database, "
        + "no files in progress and no files in the blobstore")
    void testShouldReturnEmptyWhenFolderHasHearingRecordingsWithNoSegments() {
        doReturn(Optional.of(FOLDER)).when(folderRepository).findByName(TEST_FOLDER_1_NAME);
        doReturn(Collections.emptySet()).when(blobStorage).findByFolderName(FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }


    @Test
    void testShouldReturnEmptyWhenFolderHasHearingRecordingsWithNoSegmentsByHearingResource() {
        doReturn(Optional.of(FOLDER)).when(folderRepository)
            .findByNameAndHearingSource(TEST_FOLDER_1_NAME, HEARING_RESOURCE);
        doReturn(Collections.emptySet()).when(blobStorage).findByFolderName(FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME, HEARING_RESOURCE);

        assertThat(actualFilenames).hasSameElementsAs(Collections.emptySet());
    }

    @Test
    @DisplayName("Test when files are recorded in the database and the blobstore and no files in progress")
    void testShouldReturnCompletedFilesOnly() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);

        doReturn(Set.of(FILENAME_1, FILENAME_2, FILENAME_3)).when(blobStorage)
            .findByFolderName(FOLDER.getName());

        doReturn(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getSegments()).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2, FILENAME_3));
    }

    @Test
    void testShouldReturnCompletedFilesOnlyByHearingResource() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS)).when(folderRepository)
            .findByNameAndHearingSource(TEST_FOLDER_1_NAME, HEARING_RESOURCE);

        doReturn(Set.of(FILENAME_1, FILENAME_2, FILENAME_3)).when(blobStorage)
            .findByFolderName(FOLDER.getName());

        doReturn(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getSegments()).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME, HEARING_RESOURCE);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2, FILENAME_3));
    }

    @Test
    @DisplayName("Test when files are recorded in the database and blobstore and more files in progress")
    void testShouldReturnBothCompletedAndInProgressFiles() {
        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_AND_1_JOB_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);
        doReturn(Set.of(FILENAME_1, FILENAME_2)).when(blobStorage).findByFolderName(FOLDER.getName());
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
        doReturn(Collections.emptySet()).when(blobStorage).findByFolderName(FOLDER.getName());

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_3));
    }


    @Test
    @DisplayName("Test when files are recoded in the blobstore but not in the database and more files in progress")
    void testShouldReturnInProgressFilesOnly() {
        doReturn(Optional.of(FOLDER_WITH_2_JOBS_IN_PROGRESS)).when(folderRepository).findByName(TEST_FOLDER_1_NAME);

        doReturn(Set.of(SEGMENT_1, SEGMENT_2)).when(segmentRepository)
            .findByHearingRecordingFolderName(TEST_FOLDER_1_NAME);

        doReturn(Set.of(FILENAME_3)).when(blobStorage).findByFolderName(TEST_FOLDER_1_NAME);

        Set<String> actualFilenames = folderServiceImpl.getStoredFiles(TEST_FOLDER_1_NAME);

        assertThat(actualFilenames).hasSameElementsAs(Set.of(FILENAME_1, FILENAME_2));
    }

    @Test
    @DisplayName("Should throw exception when folder not in db")
    void testShouldThrowExceptionWhenNoFolderInDB() {
        assertThatExceptionOfType(DatabaseStorageException.class).isThrownBy(() -> folderServiceImpl
            .getFolderByName("nopath"));
    }
}
