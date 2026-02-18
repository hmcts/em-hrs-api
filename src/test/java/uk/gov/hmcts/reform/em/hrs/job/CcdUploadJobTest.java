package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdUploadService;

import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CcdUploadJobTest {

    private final HearingRecordingDto recordingDto = mock(HearingRecordingDto.class);

    private final LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue = new LinkedBlockingQueue<>(1000);

    private final JobInProgressService jobInProgressService = mock(JobInProgressService.class);
    private final CcdUploadService ccdUploadService = mock(CcdUploadService.class);
    private final JobExecutionContext context = mock(JobExecutionContext.class);
    private final CcdUploadJob underTest = new CcdUploadJob(ccdUploadQueue, ccdUploadService, jobInProgressService);

    @BeforeEach
    void prepare() {
        ccdUploadQueue.clear();
    }

    @Test
    void testShouldInvokeuploadionServiceWhenHearingRecordingIsPolled() {
        ccdUploadQueue.offer(recordingDto);
        doNothing().when(ccdUploadService).upload(recordingDto);

        underTest.executeInternal(context);

        verify(ccdUploadService, times(1)).upload(recordingDto);
        verify(jobInProgressService, times(1)).deRegister(recordingDto);
    }

    @Test
    void testShouldHandleGracefullyWhenUnhandledError() {
        ccdUploadQueue.offer(recordingDto);
        doThrow(RuntimeException.class).when(ccdUploadService).upload(any(HearingRecordingDto.class));
        underTest.executeInternal(context);
        verify(ccdUploadService, times(1)).upload(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(recordingDto);
    }

    @Test
    void testNoArgsConstructorCanBeInstantiated() {
        CcdUploadJob ccdUploadJob = new CcdUploadJob();
        assertNotNull(ccdUploadJob);
    }

}
