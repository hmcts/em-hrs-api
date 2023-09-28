package uk.gov.hmcts.reform.em.hrs.config;


import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;


@Configuration
public class ClockConfig {

    public static final String EUROPE_LONDON = "Europe/London";
    public static final ZoneId EUROPE_LONDON_ZONE_ID = ZoneId.of(EUROPE_LONDON);

    @Bean
    public ClockProvider clockProvider() {
        return () -> Clock.system(EUROPE_LONDON_ZONE_ID);
    }
}
