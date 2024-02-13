package uk.gov.hmcts.reform.em.hrs.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import java.util.List;

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
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public EmServiceAuthFilter emServiceAuthFilter(ServiceAuthorisationApi authorisationApi,
                @Value("${idam.s2s-authorised.services}") List<String> authorisedServices,
                AuthenticationManager authenticationManager) {

        AuthTokenValidator authTokenValidator = new ServiceAuthTokenValidator(authorisationApi);
        EmServiceAuthFilter emServiceAuthFilter = new EmServiceAuthFilter(authTokenValidator, authorisedServices);
        emServiceAuthFilter.setAuthenticationManager(authenticationManager);
        return emServiceAuthFilter;
    }

    @Bean
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public FilterRegistrationBean<EmServiceAuthFilter> registration(EmServiceAuthFilter filter) {
        FilterRegistrationBean<EmServiceAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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
