package uk.gov.hmcts.reform.em.hrs.service.idam.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class IdamCachedClientTest {

    @Mock
    private IdamClient idamApi;

    private IdamCachedClient idamCachedClient;

    private long refreshTokenBeforeExpiry = 2879;

    private static final String JWT_1 = "JWT_1_assawdsa";
    private static final String JWT_2 = "JWT_2_56212398";

    private static final String JWT_WITH_BEARER_1 = "Bearer " + JWT_1;

    private static final String JWT_WITH_BEARER_2 = "Bearer " + JWT_2;

    private static final String USERNAME = "userxxx";

    private static final String PASSWORD = "passs123";


    private static final TokenResponse TOKEN_RESPONSE_1 = new TokenResponse(
        JWT_1,
        "28800",
        "ID_TOKEN_xxxx_123",
        "REFRESH_TOKEN_xyxyx123",
        "openid profile roles",
        "Bearer"
    );

    private static final TokenResponse TOKEN_RESPONSE_2 = new TokenResponse(
        JWT_2,
        "28800",
        "ID_TOKEN_xxxx_123",
        "REFRESH_TOKEN_xyxyx123",
        "openid profile roles",
        "Bearer"
    );

    private static final UserInfo USER_INFO = new UserInfo(
        "sub",
        "uid",
        "name",
        "givenname",
        "familyname",
        Arrays.asList("role1, role2", "role3")
    );

    @BeforeEach
    void setUp() {
        this.idamCachedClient = new IdamCachedClient(
            idamApi,
            USERNAME,
            PASSWORD,
            new IdamCacheExpiry(refreshTokenBeforeExpiry)
        );
    }

    @Test
    void should_get_credentials_when_no_error() {

        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(USER_INFO);

        CachedIdamCredential cachedIdamCredential =
            idamCachedClient.getIdamCredentials();

        assertThat(cachedIdamCredential.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential.userId).isEqualTo(USER_INFO.getUid());
        verify(idamApi).getAccessTokenResponse(USERNAME, PASSWORD);
        verify(idamApi).getUserInfo(JWT_WITH_BEARER_1);
    }


    @Test
    void should_create_token_when_cache_is_expired() {
        IdamCachedClient idamCachedClient = new IdamCachedClient(
            idamApi,
            USERNAME,
            PASSWORD,
            new IdamCacheExpiry(29999) // Sets expiry to 1 second
        );

        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD))
            .willReturn(TOKEN_RESPONSE_1, TOKEN_RESPONSE_2);

        UserInfo userDetails1 = USER_INFO;
        UserInfo userDetails2 = new UserInfo("12", "a8da9s8", "q@a.com", "", "", null);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(userDetails1);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_2)).willReturn(userDetails2);

        CachedIdamCredential initialCredentials = idamCachedClient.getIdamCredentials();
        assertThat(initialCredentials.accessToken).isEqualTo(JWT_WITH_BEARER_1);

        await()
            .atMost(4, SECONDS)
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                CachedIdamCredential credentialsAfterExpiry = idamCachedClient.getIdamCredentials();
                assertThat(credentialsAfterExpiry.accessToken).isNotEqualTo(initialCredentials.accessToken);
                assertThat(credentialsAfterExpiry.accessToken).isEqualTo(JWT_WITH_BEARER_2);
                assertThat(credentialsAfterExpiry.userId).isEqualTo(userDetails2.getUid());
            });

        verify(idamApi, times(2)).getAccessTokenResponse(any(), any());
        verify(idamApi, times(2)).getUserInfo(any());
    }

    @Test
    void should_create_new_token_when_token_removed_from_cache() {

        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1, TOKEN_RESPONSE_2);
        UserInfo expectedUserDetails1 = USER_INFO;
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(expectedUserDetails1);
        UserInfo expectedUserDetails2 = new UserInfo("1122", "add3rew", "12q@a.com", "joe", "doe", null);

        given(idamApi.getUserInfo(JWT_WITH_BEARER_2)).willReturn(expectedUserDetails2);

        CachedIdamCredential cachedIdamCredential1 = idamCachedClient.getIdamCredentials();

        idamCachedClient.removeAccessTokenFromCache(USERNAME);

        CachedIdamCredential cachedIdamCredential2 = idamCachedClient.getIdamCredentials();

        assertThat(cachedIdamCredential1.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential1.userId).isEqualTo(expectedUserDetails1.getUid());
        assertThat(cachedIdamCredential2.accessToken).isEqualTo(JWT_WITH_BEARER_2);
        assertThat(cachedIdamCredential2.userId).isEqualTo(expectedUserDetails2.getUid());

        verify(idamApi, times(2)).getAccessTokenResponse(any(), any());
        verify(idamApi, times(2)).getUserInfo(any());
    }
}
