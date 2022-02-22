package uk.gov.hmcts.reform.em.hrs.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Value("#{'${idam.s2s-authorised.services}'.split(',')}")
    private List<String> s2sNamesWhiteList;

    @Bean
    public Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor() {
        return any -> s2sNamesWhiteList;
    }

    @Configuration
    @Order(1) // Checking only for S2S Token
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {



        @Autowired
        private RequestAuthorizer<Service> serviceRequestAuthorizer;

        private AuthCheckerServiceOnlyFilter serviceOnlyFilter;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        public void setServiceOnlyFilter(Optional<AuthCheckerServiceOnlyFilter> serviceOnlyFilter) {
            this.serviceOnlyFilter = serviceOnlyFilter.orElseGet(() -> {
                AuthCheckerServiceOnlyFilter filter = new AuthCheckerServiceOnlyFilter(serviceRequestAuthorizer);
                filter.setAuthenticationManager(authenticationManager);
                return filter;
            });
        }

        @Override
        protected void configure(HttpSecurity http) {
            try {
                serviceOnlyFilter.setAuthenticationManager(authenticationManager());
                http.headers().cacheControl().disable();
                http.addFilter(serviceOnlyFilter)
                    .csrf().disable()
                    .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/segments")
                    .antMatchers(HttpMethod.GET, "/folders/**")
                    .and()
                    .authorizeRequests().anyRequest().authenticated();
            } catch (Exception e) {
                LOG.info("Error in InternalApiSecurityConfigurationAdapter: {}", e);
            }
        }

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers(
                "/swagger-ui.html",
                "/webjars/springfox-swagger-ui/**",
                "/swagger-resources/**",
                "/v2/**",
                "/health",
                "/health/liveness",
                "/health/readiness",
                "/status/health",
                "/loggers/**",
                "/");
        }
    }

    @Configuration
    @Order(2) // Checking only for Idam User Token
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private JwtAuthenticationConverter jwtAuthenticationConverter;

        private final ServiceAuthFilter serviceAuthFilter;

        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
        private String issuerUri;

        @Autowired
        public ExternalApiSecurityConfigurationAdapter(
            final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
            final ServiceAuthFilter serviceAuthFilter
        ) {
            super();
            this.serviceAuthFilter = serviceAuthFilter;
            this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        }

        @Override
        protected void configure(HttpSecurity http) {
            try {
                http.headers().cacheControl().disable();
                http.sessionManagement().sessionCreationPolicy(STATELESS).and()
                    .formLogin().disable()
                    .logout().disable()
                    .authorizeRequests()
                    .antMatchers(HttpMethod.GET, "/hearing-recordings/**").authenticated()
                    .antMatchers(HttpMethod.POST, "/sharees").authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .and()
                    .and()
                    .oauth2Client();
            } catch (Exception e) {
                LOG.info("Error in ExternalApiSecurityConfigurationAdapter: {}", e);
            }
        }

        @Bean
        JwtDecoder jwtDecoder() {
            NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
                JwtDecoders.fromOidcIssuerLocation(issuerUri);
            // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
            OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
            OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);

            jwtDecoder.setJwtValidator(validator);

            return jwtDecoder;
        }

    }

}
