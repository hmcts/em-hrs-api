package uk.gov.hmcts.reform.em.hrs.config.security;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

@Configuration
@ComponentScan(basePackages = {"uk.gov.hmcts.reform.idam.client", "uk.gov.hmcts.reform.ccd.client"})
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam.client", "uk.gov.hmcts.reform.ccd.client"})
@Profile({"!integration-web-test"})
public class AuthTokenValidationConfiguration {

    @Bean
    public AuthTokenValidator authTokenValidator(final ServiceAuthorisationApi serviceAuthorisationApi) {

        return new ServiceAuthTokenValidator(serviceAuthorisationApi);
    }

}
