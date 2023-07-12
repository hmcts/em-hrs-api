package uk.gov.hmcts.reform.em.hrs.appinsights;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppInsightConfig {

    @Bean
    public TelemetryClient getTelemetryClient() {
        return new TelemetryClient();

    }
}
