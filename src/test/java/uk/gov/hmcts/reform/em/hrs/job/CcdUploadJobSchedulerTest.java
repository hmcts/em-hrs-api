package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CcdUploadJobSchedulerTest {
    private final Scheduler scheduler = mock(Scheduler.class);

    private final int intervalInSeconds = 1;

    private final CcdUploadJobScheduler underTest = new CcdUploadJobScheduler(scheduler, intervalInSeconds);

    @Test
    void testShouldStartTheScheduler() throws Exception {
        doReturn(new Date()).when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        underTest.start();
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void testShouldStopTheScheduler() throws Exception {
        doNothing().when(scheduler).shutdown(anyBoolean());
        underTest.stop();
        verify(scheduler, times(1)).shutdown(anyBoolean());
    }
}
