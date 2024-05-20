package uk.gov.hmcts.reform.em.hrs.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.Collections;

@Configuration
@ComponentScan(basePackages = {"uk.gov.hmcts.reform.idam.client", "uk.gov.hmcts.reform.ccd.client"})
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam.client", "uk.gov.hmcts.reform.ccd.client"})
@Profile({"!integration-web-test"})
public class AuthTokenValidationConfiguration {

    @Bean
    public AuthTokenValidator authTokenValidator(final ServiceAuthorisationApi serviceAuthorisationApi) {

        return new ServiceAuthTokenValidator(serviceAuthorisationApi);
    }

    @Bean
    @ConditionalOnMissingBean(name = "preAuthenticatedAuthenticationProvider")
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider =
            new PreAuthenticatedAuthenticationProvider();
        preAuthenticatedAuthenticationProvider.setPreAuthenticatedUserDetailsService(
            token -> new User((String) token.getPrincipal(), "N/A", Collections.emptyList())
        );
        return preAuthenticatedAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider) {
        return new ProviderManager(Collections.singletonList(preAuthenticatedAuthenticationProvider));
    }


}
