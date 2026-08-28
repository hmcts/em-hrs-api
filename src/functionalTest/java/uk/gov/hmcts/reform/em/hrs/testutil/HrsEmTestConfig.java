package uk.gov.hmcts.reform.em.hrs.testutil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HrsEmTestConfig {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
