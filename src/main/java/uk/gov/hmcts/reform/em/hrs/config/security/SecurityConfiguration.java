package uk.gov.hmcts.reform.em.hrs.config.security;

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
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
@Profile({"!integration-web-test"})
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Configuration
    @Order(1) // Checking only for S2S Token
    @Profile({"!integration-web-test"})
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private ServiceAuthFilter serviceAuthFilter;

        @Override
        protected void configure(HttpSecurity http) {
            try {
                http.headers().cacheControl().disable();
                http.addFilter(serviceAuthFilter)
                    .csrf().disable()
                    .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/segments")
                    .antMatchers(HttpMethod.GET, "/folders/**");
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
    @Profile({"!integration-web-test"})
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
