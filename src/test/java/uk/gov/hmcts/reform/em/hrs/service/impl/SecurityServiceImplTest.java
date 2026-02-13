package uk.gov.hmcts.reform.em.hrs.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.em.hrs.service.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.em.hrs.service.idam.cache.IdamCachedClient;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.hrs.service.impl.SecurityServiceImpl.CLIENTIP;
import static uk.gov.hmcts.reform.em.hrs.service.impl.SecurityServiceImpl.DUMMY_NAME;
import static uk.gov.hmcts.reform.em.hrs.service.impl.SecurityServiceImpl.SERVICE_AUTH;
import static uk.gov.hmcts.reform.em.hrs.service.impl.SecurityServiceImpl.USER_AUTH;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    private static final String MOCK_USER_TOKEN = "user-token-long-enough-for-test";
    private static final String MOCK_USER_ID = "user-id";
    private static final String MOCK_S2S_TOKEN = "s2s-token-long-enough-for-test";
    private static final String MOCK_EMAIL = "test@example.com";
    private static final String MOCK_SERVICE_NAME = "hrs_service";
    private static final String BEARER_TOKEN = "Bearer " + MOCK_S2S_TOKEN;

    @Mock
    private IdamClient idmClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private AuthTokenValidator authTokenValidator;

    @Mock
    private IdamCachedClient idmCachedClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createTokensShouldReturnMapWithTokens() {
        CachedIdamCredential credential = new CachedIdamCredential(MOCK_USER_TOKEN, MOCK_USER_ID, 1000L);
        when(idmCachedClient.getIdamCredentials()).thenReturn(credential);
        when(authTokenGenerator.generate()).thenReturn(MOCK_S2S_TOKEN);

        Map<String, String> tokens = securityService.createTokens();

        assertThat(tokens)
            .containsEntry("user", MOCK_USER_TOKEN)
            .containsEntry("userId", MOCK_USER_ID)
            .containsEntry("service", MOCK_S2S_TOKEN);
    }

    @Test
    void getUserEmailShouldReturnSubFromUserInfo() {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getSub()).thenReturn(MOCK_EMAIL);
        when(idmClient.getUserInfo(MOCK_USER_TOKEN)).thenReturn(userInfo);

        String email = securityService.getUserEmail(MOCK_USER_TOKEN);

        assertThat(email).isEqualTo(MOCK_EMAIL);
    }

    @Test
    void getUserInfoShouldReturnUserInfoObject() {
        UserInfo expectedUserInfo = mock(UserInfo.class);
        when(idmClient.getUserInfo(MOCK_USER_TOKEN)).thenReturn(expectedUserInfo);

        UserInfo actualUserInfo = securityService.getUserInfo(MOCK_USER_TOKEN);

        assertThat(actualUserInfo).isEqualTo(expectedUserInfo);
    }

    @Test
    void getCurrentlyAuthenticatedServiceNameShouldReturnDummyNameWhenRequestIsNull() {
        RequestContextHolder.resetRequestAttributes();

        String serviceName = securityService.getCurrentlyAuthenticatedServiceName();

        assertThat(serviceName).isEqualTo(DUMMY_NAME);
    }

    @Test
    void getCurrentlyAuthenticatedServiceNameShouldReturnDummyNameWhenTokenIsBlank() {
        mockRequestContext();
        when(httpServletRequest.getHeader(SERVICE_AUTH)).thenReturn("");

        String serviceName = securityService.getCurrentlyAuthenticatedServiceName();

        assertThat(serviceName).isEqualTo(DUMMY_NAME);
    }

    @Test
    void getCurrentlyAuthenticatedServiceNameShouldReturnServiceNameWhenTokenHasBearerPrefix() {
        mockRequestContext();
        when(httpServletRequest.getHeader(SERVICE_AUTH)).thenReturn(BEARER_TOKEN);
        when(authTokenValidator.getServiceName(BEARER_TOKEN)).thenReturn(MOCK_SERVICE_NAME);

        String serviceName = securityService.getCurrentlyAuthenticatedServiceName();

        assertThat(serviceName).isEqualTo(MOCK_SERVICE_NAME);
    }

    @Test
    void getCurrentlyAuthenticatedServiceNameShouldReturnServiceNameWhenTokenMissingBearerPrefix() {
        mockRequestContext();
        when(httpServletRequest.getHeader(SERVICE_AUTH)).thenReturn(MOCK_S2S_TOKEN);
        when(authTokenValidator.getServiceName(BEARER_TOKEN)).thenReturn(MOCK_SERVICE_NAME);

        String serviceName = securityService.getCurrentlyAuthenticatedServiceName();

        assertThat(serviceName).isEqualTo(MOCK_SERVICE_NAME);
    }

    @Test
    void getAuditUserEmailShouldReturnHrsIngestorWhenRequestIsNull() {
        RequestContextHolder.resetRequestAttributes();

        String auditUser = securityService.getAuditUserEmail();

        assertThat(auditUser).isEqualTo("hrsIngestor");
    }

    @Test
    void getAuditUserEmailShouldReturnEmailFromTokenWhenRequestIsPresent() {
        mockRequestContext();
        when(httpServletRequest.getHeader(USER_AUTH)).thenReturn(MOCK_USER_TOKEN);

        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getSub()).thenReturn(MOCK_EMAIL);
        when(idmClient.getUserInfo(MOCK_USER_TOKEN)).thenReturn(userInfo);

        String auditUser = securityService.getAuditUserEmail();

        assertThat(auditUser).isEqualTo(MOCK_EMAIL);
    }

    @Test
    void getClientIpShouldReturnNullWhenRequestIsNull() {
        RequestContextHolder.resetRequestAttributes();

        String clientIp = securityService.getClientIp();

        assertThat(clientIp).isNull();
    }

    @Test
    void getClientIpShouldReturnIpFromHeaderWhenRequestIsPresent() {
        mockRequestContext();
        String expectedIp = "127.0.0.1";
        when(httpServletRequest.getHeader(CLIENTIP)).thenReturn(expectedIp);

        String clientIp = securityService.getClientIp();

        assertThat(clientIp).isEqualTo(expectedIp);
    }

    private void mockRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }
}
