package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHARER_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.service.SecurityServiceImpl.CLIENTIP;

@SpringBootTest(classes = {SecurityServiceImpl.class},
    properties = {"idam.system-user.username=SystemUser", "idam.system-user.password=SystemPassword"})
class SecurityServiceImplTest {

    private static final String DUMMY_NAME = "dummyName";
    private static final String HRS_INGESTOR = "hrsIngestor";
    private static final String SYSTEM_USER = "SystemUser";
    private static final String SYSTEM_USER_PASSWORD = "SystemPassword";
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UserDetails USER_DETAILS = UserDetails.builder()
        .id(USER_ID)
        .email(SHARER_EMAIL_ADDRESS)
        .build();
    private static final UserInfo USER_INFO = UserInfo.builder()
        .uid(USER_ID)
        .roles(Arrays.asList("caseworker-hrs"))
        .build();
    private static final String SERVICE_NAME = "TestService";

    @Mock
    private MockHttpServletRequest request;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private AuthTokenValidator authTokenValidator;

    @Autowired
    private SecurityServiceImpl underTest;

    @BeforeEach
    public void before() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testShouldGetUserId() {
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);

        final String userId = underTest.getUserId(AUTHORIZATION_TOKEN);

        assertThat(userId).isEqualTo(USER_ID);
        verify(idamClient, times(1)).getUserDetails(AUTHORIZATION_TOKEN);
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
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);

        final String userEmail = underTest.getUserEmail(AUTHORIZATION_TOKEN);

        assertThat(userEmail).isEqualTo(SHARER_EMAIL_ADDRESS);
        verify(idamClient, times(1)).getUserDetails(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldGetTokensMap() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);
        doReturn(SERVICE_AUTHORIZATION_TOKEN).when(authTokenGenerator).generate();

        final Map<String, String> tokens = underTest.getTokens();

        assertThat(tokens).isNotNull().isNotEmpty();
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserDetails(AUTHORIZATION_TOKEN);
        verify(authTokenGenerator, times(1)).generate();
    }

    @Test
    void testShouldDefaultGetUserId() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);

        final String userId = underTest.getUserId();

        assertThat(userId).isEqualTo(USER_ID);
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserDetails(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldDefaultGetUserEmail() {
        doReturn(AUTHORIZATION_TOKEN).when(idamClient).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);

        final String userEmail = underTest.getUserEmail();

        assertThat(userEmail).isEqualTo(SHARER_EMAIL_ADDRESS);
        verify(idamClient, times(1)).getAccessToken(SYSTEM_USER, SYSTEM_USER_PASSWORD);
        verify(idamClient, times(1)).getUserDetails(AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldDefaultGetUserInfo() {
        doReturn(USER_INFO).when(idamClient).getUserInfo(AUTHORIZATION_TOKEN);

        final UserInfo userInfo = underTest.getUserInfo(AUTHORIZATION_TOKEN);
        Assert.assertEquals(1,userInfo.getRoles().size());
        verify(idamClient, times(1)).getUserInfo(AUTHORIZATION_TOKEN);
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceNameDummyName() {
        Assert.assertEquals(SecurityServiceImpl.DUMMY_NAME, underTest.getCurrentlyAuthenticatedServiceName());
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceName() {
        doReturn("Xxxxxxxxxxxxxxxxxx").when(request).getHeader(SecurityServiceImpl.SERVICE_AUTH);
        doReturn(SERVICE_NAME).when(authTokenValidator).getServiceName(Mockito.anyString());
        Assert.assertEquals(SERVICE_NAME, underTest.getCurrentlyAuthenticatedServiceName());
    }

    @Test
    void testGetCurrentlyAuthenticatedServiceNameNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        Assert.assertEquals(DUMMY_NAME, underTest.getCurrentlyAuthenticatedServiceName());

    }

    @Test
    void testGetAuditUserEmail() {
        doReturn(AUTHORIZATION_TOKEN).when(request).getHeader(SecurityServiceImpl.USER_AUTH);
        doReturn(USER_DETAILS).when(idamClient).getUserDetails(AUTHORIZATION_TOKEN);
        Assert.assertEquals(SHARER_EMAIL_ADDRESS, underTest.getAuditUserEmail());
    }

    @Test
    void testGetAuditUserEmailNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        Assert.assertEquals(HRS_INGESTOR, underTest.getAuditUserEmail());
    }

    @Test
    void testGetClientIpNullRequest() {
        RequestContextHolder.setRequestAttributes(null);
        Assert.assertEquals(null, underTest.getClientIp());
    }

    @Test
    void testGetClientIp() {
        doReturn("127.0.0.1").when(request).getHeader(CLIENTIP);
        Assert.assertEquals("127.0.0.1", underTest.getClientIp());
    }


}
