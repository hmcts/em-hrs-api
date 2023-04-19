package uk.gov.hmcts.reform.em.hrs.smoke.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@Configuration
@TestPropertySource(value = "classpath:application.yml")
@ComponentScan("uk.gov.hmcts.reform.em.hrs.smoke")
public class SmokeTestContextConfiguration {
}
