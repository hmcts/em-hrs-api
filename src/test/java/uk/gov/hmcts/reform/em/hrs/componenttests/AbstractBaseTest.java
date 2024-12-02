package uk.gov.hmcts.reform.em.hrs.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestApplicationConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.hrs.config.WebConfig;
import uk.gov.hmcts.reform.em.hrs.config.security.JwtGrantedAuthoritiesConverter;

@SpringBootTest(classes = {TestApplicationConfig.class, TestSecurityConfiguration.class, TestAzureStorageConfig.class})
@ExtendWith({MockitoExtension.class})
public abstract class AbstractBaseTest extends AbstractDataSourceTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;

    protected MockMvc mockMvc;

    @Mock
    protected Authentication authentication;

    @Mock
    protected SecurityContext securityContext;

    @MockitoBean
    private WebConfig webConfig;

    @BeforeEach
    public void setupMocks() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
}
