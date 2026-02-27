package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTtlTaskTest {

    @Mock
    private HearingRecordingService hearingRecordingService;

    @Mock
    private CcdDataStoreApiClient ccdDataStoreApiClient;

    @InjectMocks
    private UpdateTtlTask updateTtlTask;

    private HearingRecordingTtlMigrationDTO record1;
    private final LocalDate mockTtlDate = LocalDate.now().plusYears(7);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(updateTtlTask, "batchSize", 10);
        ReflectionTestUtils.setField(updateTtlTask, "defaultThreadLimit", 2);
        ReflectionTestUtils.setField(updateTtlTask, "noOfIterations", 2);

        record1 = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(10),
            "SVC",
            "JUR",
            1111222233334444L
        );
    }

    @Test
    void shouldProcessRecordsSuccessfully() {
        // Chain thenReturn to avoid unchecked generic array creation warning
        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(List.of(record1))
            .thenReturn(Collections.emptyList());

        when(ccdDataStoreApiClient.updateCaseWithTtl(anyLong()))
            .thenReturn(mockTtlDate);

        updateTtlTask.run();

        verify(ccdDataStoreApiClient).updateCaseWithTtl(record1.ccdCaseId());
        verify(hearingRecordingService).updateTtl(record1.id(), mockTtlDate);
    }

    @Test
    void shouldDoNothingWhenNoRecordsFound() {
        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(Collections.emptyList());

        updateTtlTask.run();

        verify(hearingRecordingService, never()).updateTtl(any(), any());
        verify(ccdDataStoreApiClient, never()).updateCaseWithTtl(anyLong());
    }

    @Test
    void shouldSkipDbUpdateIfCcdFails() {
        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(List.of(record1))
            .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("CCD Unavailable"))
            .when(ccdDataStoreApiClient).updateCaseWithTtl(anyLong());

        updateTtlTask.run();

        verify(ccdDataStoreApiClient).updateCaseWithTtl(record1.ccdCaseId());
        verify(hearingRecordingService, never()).updateTtl(any(), any());
    }

    @Test
    void shouldHandleDuplicateCcdCaseIdsInSameRun() {
        HearingRecordingTtlMigrationDTO recA = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(), LocalDateTime.now(), "S", "J", 999L
        );
        HearingRecordingTtlMigrationDTO recB = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(), LocalDateTime.now(), "S", "J", 999L
        );

        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(Arrays.asList(recA, recB))
            .thenReturn(Collections.emptyList());

        when(ccdDataStoreApiClient.updateCaseWithTtl(999L))
            .thenReturn(mockTtlDate);

        updateTtlTask.run();

        // CCD should only be called once
        verify(ccdDataStoreApiClient, times(1)).updateCaseWithTtl(999L);
        // But both HRS records should be updated
        verify(hearingRecordingService).updateTtl(recA.id(), mockTtlDate);
        verify(hearingRecordingService).updateTtl(recB.id(), mockTtlDate);
    }

    @Test
    void shouldCacheCcdAcrossIterations() {
        HearingRecordingTtlMigrationDTO recA = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(), LocalDateTime.now(), "S", "J", 999L
        );
        HearingRecordingTtlMigrationDTO recB = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(), LocalDateTime.now(), "S", "J", 999L
        );

        // Iteration 1 gets recA. Iteration 2 gets recB.
        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(List.of(recA))
            .thenReturn(List.of(recB));

        when(ccdDataStoreApiClient.updateCaseWithTtl(999L))
            .thenReturn(mockTtlDate);

        updateTtlTask.run();

        // CCD should STILL only be called once despite processing in entirely different loop iterations
        verify(ccdDataStoreApiClient, times(1)).updateCaseWithTtl(999L);
        verify(hearingRecordingService).updateTtl(recA.id(), mockTtlDate);
        verify(hearingRecordingService).updateTtl(recB.id(), mockTtlDate);
    }

    @Test
    void shouldThrowExceptionForNullCcdCaseId() {
        HearingRecordingTtlMigrationDTO recNullCcd = new HearingRecordingTtlMigrationDTO(
            UUID.randomUUID(), LocalDateTime.now(), "S", "J", null
        );

        when(hearingRecordingService.getRecordingsForTtlUpdate(anyInt()))
            .thenReturn(List.of(recNullCcd))
            .thenReturn(Collections.emptyList());

        updateTtlTask.run();

        verify(ccdDataStoreApiClient, never()).updateCaseWithTtl(anyLong());
        verify(hearingRecordingService, never()).updateTtl(any(), any());
    }
}
