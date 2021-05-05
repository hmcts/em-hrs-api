package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHARER_EMAIL_ADDRESS;

@SpringBootTest(classes = {SecurityServiceImpl.class},
    properties = {"idam.system-user.username=SystemUser", "idam.system-user.password=SystemPassword"})
class SecurityServiceImplTest {
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Inject
    private SecurityServiceImpl underTest;

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

}
