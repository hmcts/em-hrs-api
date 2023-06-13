package uk.gov.hmcts.reform.em.hrs.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@EnableWebSecurity
@Profile({"!integration-web-test"})
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
    @Profile({"!integration-web-test"})
    public static class InternalApiSecurityConfigurationAdapter {

        private AuthCheckerServiceOnlyFilter serviceOnlyFilter;

        @Autowired
        private ServiceAuthFilter serviceAuthFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {

            http.headers(h -> h.cacheControl(c -> c.disable()))
                .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                .csrf().disable();
            return http.build();
        }

        @Bean
        public WebSecurityCustomizer getWebSecurityCustomizer() {
            return (web) -> web.ignoring().requestMatchers(
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
    @Profile({"!integration-web-test"})
    public static class ExternalApiSecurityConfigurationAdapter {

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

        @Bean
        protected SecurityFilterChain configureHttpSecurity(HttpSecurity http) throws Exception {
            http.headers(h -> h.cacheControl(c -> c.disable()))
                .csrf().disable()
                .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
                .formLogin(login -> login.disable())
                .logout(lgout -> lgout.disable())
                .authorizeRequests()
                .requestMatchers(HttpMethod.GET, "/hearing-recordings/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/sharees").authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .and()
                .and()
                .oauth2Client();
            return http.build();
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
