package uk.gov.hmcts.reform.em.hrs.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile({"!integration-web-test"})
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
}
