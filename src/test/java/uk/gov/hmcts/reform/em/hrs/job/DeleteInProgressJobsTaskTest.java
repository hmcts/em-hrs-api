package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeleteInProgressJobsTaskTest {

    private final ArgumentCaptor<LocalDateTime> localDateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    @Test
    void should_call_summary_report_service() {
        // given
        var hearingRecordingRepository = mock(HearingRecordingRepository.class);
        var jobInProgressRepository = mock(JobInProgressRepository.class);
        LocalDateTime fourHoursAgo = LocalDateTime.now(Clock.systemUTC()).minusHours(4);
        var task = new DeleteInProgressJobsTask(
            hearingRecordingRepository,
            jobInProgressRepository,
            4
        );

        // when
        task.run();

        // then
        verify(hearingRecordingRepository, times(1))
            .deleteStaleRecordsWithNullCcdCaseId(localDateCaptor.capture());
        assertThat(localDateCaptor.getValue()).isAfter(fourHoursAgo);
        verify(jobInProgressRepository, times(1))
            .deleteByCreatedOnLessThan(localDateCaptor.capture());
        assertThat(localDateCaptor.getValue()).isAfter(fourHoursAgo);

    }

    @Test
    void should_handle_exception_when_deleting_jobs_and_not_throw() {
        var hearingRecordingRepository = mock(HearingRecordingRepository.class);
        var jobInProgressRepository = mock(JobInProgressRepository.class);

        doThrow(new RuntimeException("Database connection failed"))
            .when(jobInProgressRepository).deleteByCreatedOnLessThan(any(LocalDateTime.class));

        var task = new DeleteInProgressJobsTask(
            hearingRecordingRepository,
            jobInProgressRepository,
            4
        );

        assertDoesNotThrow(task::run);

        verify(jobInProgressRepository, times(1))
            .deleteByCreatedOnLessThan(any(LocalDateTime.class));
        verify(hearingRecordingRepository, never())
            .deleteStaleRecordsWithNullCcdCaseId(any(LocalDateTime.class));
    }
}
