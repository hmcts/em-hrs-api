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
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobInProgressServiceImplTest {

    private static final String TEST_FILENAME = "test-file.mp4";
    private static final String TEST_FOLDER_NAME = "test-folder";

    @Mock
    private JobInProgressRepository jobInProgressRepository;

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private JobInProgressServiceImpl jobInProgressService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void registerWhenFolderExistsShouldSaveJobInProgress() throws DatabaseStorageException {
            HearingRecordingDto hrDto = HearingRecordingDto.builder()
                .filename(TEST_FILENAME)
                .folder(TEST_FOLDER_NAME)
                .build();

            Folder folder = Folder.builder().name(TEST_FOLDER_NAME).build();
            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.of(folder));

            jobInProgressService.register(hrDto);

            ArgumentCaptor<JobInProgress> jobCaptor = ArgumentCaptor.forClass(JobInProgress.class);
            verify(jobInProgressRepository).save(jobCaptor.capture());

            JobInProgress savedJob = jobCaptor.getValue();
            assertThat(savedJob.getFilename()).isEqualTo(TEST_FILENAME);
            assertThat(savedJob.getFolder()).isEqualTo(folder);
            assertThat(savedJob.getCreatedOn()).isNotNull();
        }

        @Test
        void registerWhenFolderDoesNotExistShouldThrowDatabaseStorageException() {
            HearingRecordingDto hrDto = HearingRecordingDto.builder()
                .filename(TEST_FILENAME)
                .folder(TEST_FOLDER_NAME)
                .build();

            when(folderRepository.findByName(TEST_FOLDER_NAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobInProgressService.register(hrDto))
                .isInstanceOf(DatabaseStorageException.class)
                .hasMessage("IllegalState - Folder not found in DB: " + TEST_FOLDER_NAME);

            verify(jobInProgressRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deRegister")
    class DeRegister {

        @Test
        void deRegisterWhenJobsExistShouldDeleteAllMatchingJobs() {
            HearingRecordingDto hrDto = HearingRecordingDto.builder()
                .filename(TEST_FILENAME)
                .folder(TEST_FOLDER_NAME)
                .build();

            JobInProgress job1 = JobInProgress.builder().filename(TEST_FILENAME).build();
            JobInProgress job2 = JobInProgress.builder().filename(TEST_FILENAME).build();
            Set<JobInProgress> jobs = Set.of(job1, job2);

            when(jobInProgressRepository.findByFolderNameAndFilename(TEST_FOLDER_NAME, TEST_FILENAME))
                .thenReturn(jobs);

            jobInProgressService.deRegister(hrDto);

            verify(jobInProgressRepository, times(1)).delete(job1);
            verify(jobInProgressRepository, times(1)).delete(job2);
        }

        @Test
        void deRegisterWhenNoJobsExistShouldDoNothing() {
            HearingRecordingDto hrDto = HearingRecordingDto.builder()
                .filename(TEST_FILENAME)
                .folder(TEST_FOLDER_NAME)
                .build();

            when(jobInProgressRepository.findByFolderNameAndFilename(TEST_FOLDER_NAME, TEST_FILENAME))
                .thenReturn(Collections.emptySet());

            jobInProgressService.deRegister(hrDto);

            verify(jobInProgressRepository, never()).delete(any());
        }
    }
}
