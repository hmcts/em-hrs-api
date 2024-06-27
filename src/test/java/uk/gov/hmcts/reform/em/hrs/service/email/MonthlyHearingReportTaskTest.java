package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MonthlyHearingReportTaskTest {


    @Test
    void should_call_summary_report_service() {
        // given
        var hearingReportEmailService = mock(HearingReportEmailService.class);
        var task = new MonthlyHearingReportTask(hearingReportEmailService);

        // when
        task.run();

        // then
        verify(hearingReportEmailService, times(1)).sendReport();;
    }
}
