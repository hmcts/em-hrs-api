package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.util.IngestionQueue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CASE_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_DATETIME;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;

class IngestionJobTest {
    private final IngestionQueue ingestionQueue = IngestionQueue.INSTANCE;

    private final IngestionService ingestionService = mock(IngestionService.class);

    private final JobExecutionContext context = mock(JobExecutionContext.class);

    private final IngestionJob underTest = new IngestionJob(ingestionQueue, ingestionService);

    private static final HearingRecordingDto HEARING_RECORDING_DTO = new HearingRecordingDto(
        CASE_REFERENCE,
        "CVP",
        "123",
        null,
        "JC",
        "LC",
        RECORDING_REFERENCE,
        "recording-cvp-uri",
        "hearing-recording-file-name",
        "mp4",
        123456789L,
        0,
        "erI2foA30B==",
        RECORDING_DATETIME
    );

    @BeforeEach
    void prepare() {
        ingestionQueue.clear();
    }

    @Test
    void testShouldInvokeIngestionServiceWhenHearingRecordingIsPolled() throws Exception {
        ingestionQueue.offer(HEARING_RECORDING_DTO);
        doNothing().when(ingestionService).ingest(HEARING_RECORDING_DTO);

        underTest.executeInternal(context);

        verify(ingestionService, times(1)).ingest(HEARING_RECORDING_DTO);
    }

    @Test
    void testShouldNotInvokeIngestionServiceWhenNullIsPolled() throws Exception {
        doNothing().when(ingestionService).ingest(any(HearingRecordingDto.class));

        underTest.executeInternal(context);

        verify(ingestionService, never()).ingest(any(HearingRecordingDto.class));
    }
}
