package uk.gov.hmcts.reform.em.hrs.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class HearingReportEmailServiceConfig {
    @Bean(name = "monthlyHearingEmailService")
    @Lazy
    public HearingReportEmailService monthlyHearingEmailService(
        EmailSender emailSender,
        @Value("${report.monthly-hearing.recipients}") String[] recipients,
        @Value("${report.monthly-hearing.from}") String from
    ) {
        return new HearingReportEmailService(
            emailSender,
            recipients,
            from,
            "Monthly hearing report for ",
            "Monthly-hearing-report-"
        );
    }

    @Bean(name = "monthlyAuditEmailService")
    @Lazy
    public HearingReportEmailService monthlyAuditEmailService(
        EmailSender emailSender,
        @Value("${report.monthly-audit.recipients}") String[] recipients,
        @Value("${report.monthly-audit.from}") String from
    ) {
        return new HearingReportEmailService(
            emailSender,
            recipients,
            from,
            "Monthly audit report for ",
            "Monthly-audit-report-"
        );
    }
}
