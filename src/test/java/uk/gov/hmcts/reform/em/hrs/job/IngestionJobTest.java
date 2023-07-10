package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_DATETIME;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;


class IngestionJobTest {
    private static final HearingRecordingDto HEARING_RECORDING_DTO = HearingRecordingDto.builder()
        .caseRef(CASE_REFERENCE)
        .recordingSource("CVP")
        .courtLocationCode("LC")
        .jurisdictionCode("JC")
        .hearingRoomRef("123")
        .recordingRef(RECORDING_REFERENCE)
        .filename("hearing-recording-file-name")
        .recordingDateTime(RECORDING_DATETIME)
        .filenameExtension("mp4")
        .fileSize(123456789L)
        .segment(0)
        .sourceBlobUrl("recording-cvp-uri")
        .checkSum("erI2foA30B==")
        .build();

    private LinkedBlockingQueue<HearingRecordingDto> ingestionQueue =
        new LinkedBlockingQueue<HearingRecordingDto>(1000);

    @SuppressWarnings("unchecked")
    private LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue = mock(LinkedBlockingQueue.class);

    private final IngestionService ingestionService = mock(IngestionService.class);
    private final JobInProgressService jobInProgressService = mock(JobInProgressService.class);
    private final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);

    private final IngestionJob underTest =
        new IngestionJob(ingestionQueue, ingestionService, jobInProgressService, ccdUploadQueue);

    @BeforeEach
    void prepare() {
        ingestionQueue.clear();
    }

    @Test
    void testShouldInvokeIngestionServiceAndRegisterJobInProgressAndCreatCcdUploadJobWhenHearingRecordingIsPolled() {
        ingestionQueue.offer(HEARING_RECORDING_DTO);
        doNothing().when(ingestionService).ingest(HEARING_RECORDING_DTO);

        underTest.executeInternal(jobExecutionContext);
        verify(jobInProgressService, times(1)).register(HEARING_RECORDING_DTO);
        verify(ingestionService, times(1)).ingest(HEARING_RECORDING_DTO);
        verify(ccdUploadQueue).offer(HEARING_RECORDING_DTO);
    }


    @Test
    void testShouldNotInvokeIngestionServiceWhenNullIsPolled() {
        doNothing().when(ingestionService).ingest(any(HearingRecordingDto.class));

        underTest.executeInternal(jobExecutionContext);

        verify(jobInProgressService, never()).register(any(HearingRecordingDto.class));
        verify(ingestionService, never()).ingest(any(HearingRecordingDto.class));
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }


    @Test
    void testShouldHandleGracefullyWhenAysncQueueIsFull() {
        ingestionQueue.offer(HEARING_RECORDING_DTO);
        doThrow(RejectedExecutionException.class).when(ingestionService).ingest(any(HearingRecordingDto.class));
        underTest.executeInternal(jobExecutionContext);
        verify(jobInProgressService, times(1)).register(HEARING_RECORDING_DTO);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(HEARING_RECORDING_DTO);
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldHandleGracefullyWhenUnhandledError() {
        ingestionQueue.offer(HEARING_RECORDING_DTO);
        doThrow(RuntimeException.class).when(ingestionService).ingest(any(HearingRecordingDto.class));
        underTest.executeInternal(jobExecutionContext);
        verify(jobInProgressService, times(1)).register(HEARING_RECORDING_DTO);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(HEARING_RECORDING_DTO);
        verify(ccdUploadQueue, never()).offer(any(HearingRecordingDto.class));
    }

    @Test
    void testShouldHandleCcdQueueFullGracefully() {
        ingestionQueue.offer(HEARING_RECORDING_DTO);
        doNothing().when(ingestionService).ingest(any(HearingRecordingDto.class));
        doReturn(false).when(ccdUploadQueue).offer(HEARING_RECORDING_DTO);
        underTest.executeInternal(jobExecutionContext);
        verify(jobInProgressService, times(1)).register(HEARING_RECORDING_DTO);
        verify(ingestionService, times(1)).ingest(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(HEARING_RECORDING_DTO);
        verify(ccdUploadQueue).offer(HEARING_RECORDING_DTO);
    }


}
