package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryReportTaskTest {

    @Test
    void should_call_summary_report_service() {
        // given
        var summaryReportService = mock(SummaryReportService.class);
        var task = new SummaryReportTask(summaryReportService);

        // when
        task.run();

        // then
        verify(summaryReportService, times(1)).process();;
    }
}
