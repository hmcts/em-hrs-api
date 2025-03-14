package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.em.hrs.config.JacksonMappingConfig;
import uk.gov.hmcts.reform.em.hrs.service.ScheduledTaskRunner;

@ActiveProfiles("integration-web-test")
@Import(JacksonMappingConfig.class)
@ImportAutoConfiguration(exclude = { OAuth2ClientAutoConfiguration.class })
public class BaseWebTest {

    protected MockMvc mockMvc;

    @MockitoBean
    public ScheduledTaskRunner taskRunner;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build();
    }

}
