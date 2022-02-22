package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeleteInProgressJobsTaskTest {

    private ArgumentCaptor<LocalDateTime> localDateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    @Test
    void should_call_summary_report_service() {
        // given
        var hearingRecordingRepository = mock(HearingRecordingRepository.class);
        var jobInProgressRepository = mock(JobInProgressRepository.class);
        LocalDateTime fourHoursAgo = LocalDateTime.now().minusHours(4);
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
}
