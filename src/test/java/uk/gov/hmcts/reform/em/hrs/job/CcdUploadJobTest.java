package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdUploadService;

import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_DATETIME;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;

class CcdUploadJobTest {
    private static final HearingRecordingDto HEARING_RECORDING_DTO = HearingRecordingDto.builder()
        .caseRef(CASE_REFERENCE)
        .recordingSource(HearingSource.CVP)
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

    private LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue = new LinkedBlockingQueue<>(1000);

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
        ccdUploadQueue.offer(HEARING_RECORDING_DTO);
        doNothing().when(ccdUploadService).upload(HEARING_RECORDING_DTO);

        underTest.executeInternal(context);

        verify(ccdUploadService, times(1)).upload(HEARING_RECORDING_DTO);
        verify(jobInProgressService, times(1)).deRegister(HEARING_RECORDING_DTO);
    }

    @Test
    void testShouldHandleGracefullyWhenUnhandledError() {
        ccdUploadQueue.offer(HEARING_RECORDING_DTO);
        doThrow(RuntimeException.class).when(ccdUploadService).upload(any(HearingRecordingDto.class));
        underTest.executeInternal(context);
        verify(ccdUploadService, times(1)).upload(any(HearingRecordingDto.class));
        verify(jobInProgressService, times(1)).deRegister(HEARING_RECORDING_DTO);
    }


}
