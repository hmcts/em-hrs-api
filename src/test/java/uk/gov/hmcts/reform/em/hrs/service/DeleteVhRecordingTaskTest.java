package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteVhRecordingTaskTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
    @Mock
    private ShareesRepository shareesRepository;

    @Mock
    private ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository;

    @InjectMocks
    private DeleteVhRecordingTask deleteVhRecordingTask;

    @Test
    void run_ShouldDeleteVhRecordings_WhenCalled() {
        var uuid = UUID.randomUUID();
        var hearingRecording = new HearingRecording();
        hearingRecording.setCcdCaseId(1234567890L);
        when(hearingRecordingRepository.listVhRecordingsToDelete()).thenReturn(List.of(uuid));
        when(hearingRecordingRepository.findById(uuid)).thenReturn(Optional.of(hearingRecording));
        deleteVhRecordingTask.run();
        verify(hearingRecordingRepository, times(1))
            .listVhRecordingsToDelete();
        verify(hearingRecordingShareeAuditEntryRepository, times(1))
            .deleteByCaseRef(1234567890L);
        verify(shareesRepository, times(1)).deleteByHearingRecordingId(uuid);
        verify(hearingRecordingRepository, times(1))
            .deleteById(uuid);
    }
}
