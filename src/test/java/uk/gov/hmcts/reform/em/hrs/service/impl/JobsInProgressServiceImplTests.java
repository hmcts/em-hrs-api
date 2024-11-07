package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FILENAME_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_1_NAME;


@ExtendWith(MockitoExtension.class)
class JobsInProgressServiceImplTests {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private JobInProgressRepository jobInProgressRepository;

    @InjectMocks
    private JobInProgressServiceImpl jobInProgressServiceImpl;

    @BeforeEach
    void prepare() {
        lenient().doNothing().when(jobInProgressRepository).deleteAll();
    }


    @Test
    @DisplayName("When folder does not exist in database, an exception should be thrown and no entry saved")
    void testRegisterWithNonExistantFolder() {
        /*
        When an ingestor checks a folder for what files shouldn't be ingested, if the folder is missing it should
        get created in the database.

        There are no legal flows whereby an ingestor would try to upload a file without a folder in place in the db.
         */

        doReturn(Optional.empty()).when(folderRepository).findByName(TEST_FOLDER_1_NAME);
        assertThatExceptionOfType(DatabaseStorageException.class).isThrownBy(() -> jobInProgressServiceImpl.register(
            HEARING_RECORDING_DTO));

        verify(jobInProgressRepository, never()).save(any(JobInProgress.class));
    }


    @Test
    @DisplayName("A folder should be found from the incoming dto, and an entry saved to the jobs in progress database")
    void testRegisterHappyPath() {
        /*
        When a folder is found in the db, the job should be persisted
         */

        doReturn(Optional.of(FOLDER_WITH_SEGMENTS_1_2_3_AND_NO_JOBS_IN_PROGRESS)).when(folderRepository)
            .findByName(TEST_FOLDER_1_NAME);
        jobInProgressServiceImpl.register(HEARING_RECORDING_DTO);

        verify(jobInProgressRepository, times(1)).save(any(JobInProgress.class));

    }


    @Test
    @DisplayName("A folder should be found from the incoming dto, and entries in the jobs in progress repo deleted")
    void testDeRegisterHappyPath() {
        /*
        When a dto is deregistered, any jobsInProgress matching the filename and foldername should be deleted.

        Its theoretically possible that there are 2 jobsInProgress in the db relating to the same DTO due to clustering
        which would currently create duplicate entries in the jobsInProgress in progress database (assumption)
        purpose of the inprogress table is to complement the main database segments in the "find files in folder"

        IE deleting all matches against filename + folder is fine
         */

        Set<JobInProgress> jobsInProgress = Set.of(JobInProgress.builder().filename(FILENAME_1).build());

        doReturn(jobsInProgress).when(jobInProgressRepository)
            .findByFolderNameAndFilename(HEARING_RECORDING_DTO.getFolder(), HEARING_RECORDING_DTO.getFilename());

        jobInProgressServiceImpl.deRegister(HEARING_RECORDING_DTO);

        verify(jobInProgressRepository, times(1)).delete(any(JobInProgress.class));

    }


}
