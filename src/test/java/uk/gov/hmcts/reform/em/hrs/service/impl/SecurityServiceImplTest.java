package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHARER_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.service.impl.SecurityServiceImpl.CLIENTIP;

@SpringBootTest(classes = {SecurityServiceImpl.class},
    properties = {"idam.system-user.username=SystemUser", "idam.system-user.password=SystemPassword"})
class SecurityServiceImplTest {

    private static final String DUMMY_NAME = "dummyName";
    private static final String HRS_INGESTOR = "hrsIngestor";
    private static final String SYSTEM_USER = "SystemUser";
    private static final String SYSTEM_USER_PASSWORD = "SystemPassword";
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UserInfo USER_INFO = UserInfo.builder()
        .sub(SHARER_EMAIL_ADDRESS)
        .uid(USER_ID)
        .roles(List.of("caseworker-hrs-searcher"))
        .build();
    private static final String SERVICE_NAME = "TestService";

    @Mock
    private MockHttpServletRequest request;

    @MockitoBean
    private IdamClient idamClient;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private AuthTokenValidator authTokenValidator;

    @Autowired
    private SecurityServiceImpl underTest;

    @BeforeEach
    public void before() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testShouldGetUserId() {
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final String userId = underTest.getUserId(AUTHORIZATION_TOKEN);

        assertThat(userId).isEqualTo(USER_ID);
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldGetUserToken() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);

        final String userToken = underTest.getUserToken();

        assertThat(userToken).isEqualTo(AUTHORIZATION_TOKEN);
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
    }

    @Test
    void testShouldGetUserEmail() {
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final String userEmail = underTest.getUserEmail(AUTHORIZATION_TOKEN);

        assertThat(userEmail).isEqualTo(SHARER_EMAIL_ADDRESS);
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldGetTokensMap() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);
        doReturn(SERVICE_AUTHORIZATION_TOKEN).when(authTokenGenerator).generate();

        final Map<String, String> tokens = underTest.getTokens();

        assertThat(tokens).isNotNull().isNotEmpty();
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
        verify(authTokenGenerator, times(1)).generate();
    }

    @Test
    void testShouldDefaultGetUserId() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final String userId = underTest.getUserId();

        assertThat(userId).isEqualTo(USER_ID);
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldDefaultGetUserEmail() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final String userEmail = underTest.getUserEmail();

        assertThat(userEmail).isEqualTo(SHARER_EMAIL_ADDRESS);
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldDefaultGetUserInfo() {
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final UserInfo userInfo = underTest.getUserInfo(AUTHORIZATION_TOKEN);
        assertEquals(1, userInfo.getRoles().size());
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceNameDummyName() {
        assertEquals(SecurityServiceImpl.DUMMY_NAME, underTest.getCurrentlyAuthenticatedServiceName());
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceName() {
        doReturn("Xxxxxxxxxxxxxxxxxx").when(request).getHeader(SecurityServiceImpl.SERVICE_AUTH);
        doReturn(SERVICE_NAME).when(authTokenValidator).getServiceName(Mockito.anyString());
        assertEquals(SERVICE_NAME, underTest.getCurrentlyAuthenticatedServiceName());
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceNameNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        assertEquals(DUMMY_NAME, underTest.getCurrentlyAuthenticatedServiceName());

    }

    @Test
    void testGetAuditUserEmail() {
        doReturn(AUTHORIZATION_TOKEN).when(request).getHeader(SecurityServiceImpl.USER_AUTH);
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);
        assertEquals(SHARER_EMAIL_ADDRESS, underTest.getAuditUserEmail());
    }

    @Test
    void testGetAuditUserEmailNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        assertEquals(HRS_INGESTOR, underTest.getAuditUserEmail());
    }

    @Test
    void testGetClientIpNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        assertEquals(null, underTest.getClientIp());
    }

    @Test
    void testGetClientIp() {
        doReturn("127.0.0.1").when(request).getHeader(CLIENTIP);
        assertEquals("127.0.0.1", underTest.getClientIp());
    }


}
