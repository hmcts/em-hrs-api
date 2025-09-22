package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

@Configuration
@EnableJpaAuditing
public class PersistenceConfig {

    @Bean
    AuditorAware<String> auditorProvider(SecurityService securityService) {
        return new AuditorAwareImpl(securityService);
    }
}
