package uk.gov.hmcts.reform.em.hrs.helper;


import jakarta.validation.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static uk.gov.hmcts.reform.em.hrs.config.ClockConfig.EUROPE_LONDON_ZONE_ID;

@TestConfiguration
public class TestClockProvider {

    @SuppressWarnings({"java:S1170", "java:S1444"}) // Field needs to be mutable for test clock control
    public static Instant stoppedInstant = Instant.now();

    @Bean
    @Primary
    public ClockProvider stoppedClock() {
        return () -> provideClock(EUROPE_LONDON_ZONE_ID);
    }

    private Clock provideClock(ZoneId zoneId) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zoneId;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return provideClock(zoneId);
            }

            @Override
            public Instant instant() {
                if (stoppedInstant == null) {
                    return ZonedDateTime.now(zoneId).toInstant();
                } else {
                    return stoppedInstant;
                }
            }
        };
    }
}
