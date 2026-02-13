package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class IngestionJobTest {

    private final HearingRecordingDto recordingDto = mock(HearingRecordingDto.class);

    private final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue =
        new LinkedBlockingQueue<>(1000);

    @SuppressWarnings("unchecked")
    private final LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue = mock(LinkedBlockingQueue.class);

    private final IngestionService ingestionService = mock(IngestionService.class);
    private final JobInProgressService jobInProgressService = mock(JobInProgressService.class);
    private final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);

    private final IngestionJob underTest =
        new IngestionJob(ingestionQueue, ingestionService, jobInProgressService, ccdUploadQueue);

    @BeforeEach
    void prepare() {
        ingestionQueue.clear();
        doNothing().when(ingestionService).ingest(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldSuccessfullyProcessIngestionAndQueueForCcdUpload() {
        ingestionQueue.offer(recordingDto);
        doReturn(true).when(ccdUploadQueue).offer(recordingDto);

        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, times(1)).register(recordingDto);
        verify(ingestionService, times(1)).ingest(recordingDto);
        verify(ccdUploadQueue, times(1)).offer(recordingDto);
        verify(jobInProgressService, never()).deRegister(any());
    }

    @Test
    void testShouldNotInvokeAnyServiceWhenIngestionQueueIsEmpty() {
        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, never()).register(any(HearingRecordingDto.class));
        verify(ingestionService, never()).ingest(any(HearingRecordingDto.class));
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldHandleGracefullyWhenAysncQueueIsFull() {
        ingestionQueue.offer(recordingDto);
        doThrow(RejectedExecutionException.class).when(ingestionService).ingest(any(HearingRecordingDto.class));

        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, times(1)).register(recordingDto);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(recordingDto);
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldHandleGracefullyWhenUnhandledError() {
        ingestionQueue.offer(recordingDto);
        doThrow(RuntimeException.class).when(ingestionService).ingest(any(HearingRecordingDto.class));

        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, times(1)).register(recordingDto);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(recordingDto);
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldHandleCcdQueueFullGracefully() {
        ingestionQueue.offer(recordingDto);
        doReturn(false).when(ccdUploadQueue).offer(recordingDto);

        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, times(1)).register(recordingDto);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(ccdUploadQueue, times(1)).offer(recordingDto);
        verify(jobInProgressService, times(1)).deRegister(recordingDto);
    }

    @Test
    void testNoArgsConstructorCanBeInstantiated() {
        IngestionJob ingestionJob = new IngestionJob();
        assertThat(ingestionJob).isNotNull();
    }
}
