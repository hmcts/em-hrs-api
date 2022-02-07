package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

class SummaryReportServiceTest {
    private SummaryReportService summaryReportService = new SummaryReportService();

    @Test
    void should_process() {
        summaryReportService.process();
    }
}
