package uk.gov.hmcts.reform.em.hrs.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile({"!integration-web-test"})
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    private final EmServiceAuthFilter emServiceAuthFilter;

    public SecurityConfiguration(final EmServiceAuthFilter emServiceAuthFilter) {
        this.emServiceAuthFilter = emServiceAuthFilter;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
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
            "/report/**",
            "/"
        );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain s2sFilterChain(HttpSecurity http) throws Exception {
        //        http.securityMatcher("/segments", "/folders/*");
        http.headers(httpSecurityHeadersConfigurer ->
                         httpSecurityHeadersConfigurer.cacheControl(HeadersConfigurer.CacheControlConfig::disable));

        http.securityMatchers(requestMatcherConfigurer ->
                                  requestMatcherConfigurer.requestMatchers("/segments", "/folders/*"))
            .addFilterBefore(emServiceAuthFilter, AnonymousAuthenticationFilter.class)
            .sessionManagement(httpSecuritySessionManagementConfigurer ->
                                   httpSecuritySessionManagementConfigurer.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                                       authorizationManagerRequestMatcherRegistry.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .addFilterBefore(emServiceAuthFilter, BearerTokenAuthenticationFilter.class)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(matcherRegistry ->
                                       matcherRegistry.requestMatchers(
                                           HttpMethod.GET,
                                           "/hearing-recordings/**"
                                       ).authenticated())
            .authorizeHttpRequests(matcherRegistry ->
                                       matcherRegistry.requestMatchers(HttpMethod.POST, "/sharees").authenticated())
            .authorizeHttpRequests(matcherRegistry ->
                                       matcherRegistry.requestMatchers("/error").authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
            .oauth2Client(withDefaults());
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
