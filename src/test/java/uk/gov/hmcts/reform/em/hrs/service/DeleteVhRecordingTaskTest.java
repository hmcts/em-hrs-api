package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteVhRecordingTaskTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;

    @InjectMocks
    private DeleteVhRecordingTask deleteVhRecordingTask;

    @Test
    void run_ShouldDeleteVhRecordings_WhenCalled() {
        deleteVhRecordingTask.run();
        verify(hearingRecordingRepository, times(1))
            .listVhRecordingsToDelete();
    }
}
