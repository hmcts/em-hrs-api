package uk.gov.hmcts.reform.em.hrs.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDate;
import java.util.function.Function;

@Configuration
public class HearingReportEmailServiceConfig {

    public static Function<LocalDate, String> monthlyReportAttachmentName(String prefix) {
        return reportDate -> prefix + reportDate.getMonth() + "-" + reportDate.getYear() + ".csv";
    }

    public static Function<LocalDate, String> weeklyReportAttachmentName(String prefix) {
        return reportDate -> prefix + reportDate.minusDays(7) + ".csv";
    }

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
            monthlyReportAttachmentName("Monthly-hearing-report-")
        );
    }


    @Bean(name = "weeklyHearingEmailService")
    @Lazy
    public HearingReportEmailService weeklyHearingEmailService(
        EmailSender emailSender,
        @Value("${report.weekly-hearing.recipients}") String[] recipients,
        @Value("${report.weekly-hearing.from}") String from
    ) {
        return new HearingReportEmailService(
            emailSender,
            recipients,
            from,
            "Weekly hearing report for ",
            weeklyReportAttachmentName("Weekly-hearing-report-from-")
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
            monthlyReportAttachmentName("Monthly-audit-report-")
        );
    }
}
