package uk.gov.hmcts.reform.em.hrs.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.em.hrs.Application;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.hrs.config.security.JwtGrantedAuthoritiesConverter;

import javax.inject.Inject;

@SpringBootTest(classes = {
    TestSecurityConfiguration.class,
    TestAzureStorageConfig.class,
    Application.class}
)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractBaseTest extends AbstractDataSourceTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;

    protected MockMvc mockMvc;

    @Mock
    protected Authentication authentication;

    @Mock
    protected SecurityContext securityContext;

    @BeforeEach
    public void setupMocks() {
        // doReturn(authentication).when(securityContext).getAuthentication();
        // SecurityContextHolder.setContext(securityContext);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
}
