package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteVhRecordingTaskTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
    @Mock
    private ShareesRepository shareesRepository;
    @InjectMocks
    private DeleteVhRecordingTask deleteVhRecordingTask;

    @Test
    void run_ShouldDeleteVhRecordings_WhenCalled() {
        var uuid = UUID.randomUUID();
        when(hearingRecordingRepository.listVhRecordingsToDelete()).thenReturn(List.of(uuid));
        deleteVhRecordingTask.run();
        verify(hearingRecordingRepository, times(1))
            .listVhRecordingsToDelete();
        verify(shareesRepository, times(1)).deleteByHearingRecordingId(uuid);
        verify(hearingRecordingRepository, times(1))
            .deleteById(uuid);
    }
}
