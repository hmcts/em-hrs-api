package uk.gov.hmcts.reform.em.hrs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Built-in feature which saves service's swagger specs in temporary directory.
 * Each travis run on master should automatically save and upload (if updated) documentation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
    FlywayAutoConfiguration.class
})
@ActiveProfiles("integration-web-test-2")
@Import(TestAzureStorageConfig.class)
class OpenAPIPublisherTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdamClient idamClient;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/v3/api-docs/**", // This is the crucial line
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
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }


    @DisplayName("Generate swagger documentation")
    @Test
    void generateDocs() throws Exception {
        byte[] specs = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs.json"))) {
            outputStream.write(specs);
        }
    }
}
