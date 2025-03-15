package uk.gov.hmcts.reform.em.hrs.service.idam.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

/**
 * A client that caches IDAM credentials.
 */
@Service
public class IdamCachedClient {

    public static final String BEARER_AUTH_TYPE = "Bearer ";

    private Cache<String, CachedIdamCredential> idamCache;

    private final IdamClient idamClient;
    private final String systemUsername;
    private final String systemUserPassword;

    /**
     * Constructor for IdamCachedClient.
     * @param idamClient The IDAM client
     * @param systemUsername The user name
     * @param systemUserPassword The users password
     * @param idamCacheExpiry The IDAM cache expiry
     */
    public IdamCachedClient(
        IdamClient idamClient,
        final @Value("${idam.system-user.username}") String systemUsername,
        final @Value("${idam.system-user.password}") String systemUserPassword,
        IdamCacheExpiry idamCacheExpiry
    ) {
        this.idamClient = idamClient;
        this.systemUsername = systemUsername;
        this.systemUserPassword = systemUserPassword;
        this.idamCache = Caffeine.newBuilder()
            .expireAfter(idamCacheExpiry)
            .build();
    }

    /**
     * Gets the IDAM credentials for the given jurisdiction.
     * @return The IDAM credentials
     */
    public CachedIdamCredential getIdamCredentials() {
        return this.idamCache.get(this.systemUsername, this::retrieveIdamInfo);
    }

    /**
     * Removes the access token from the cache for the given jurisdiction.
     * @param key The username or token
     */
    public void removeAccessTokenFromCache(String key) {
        this.idamCache.invalidate(key);
    }

    /**
     * Retrieves the IDAM credentials for the given jurisdiction.
     * @param key The username or token
     * @return The IDAM credentials
     */
    private CachedIdamCredential retrieveIdamInfo(String key) {
        TokenResponse tokenResponse = idamClient
            .getAccessTokenResponse(
                key,
                this.systemUserPassword
            );

        String tokenWithBearer = BEARER_AUTH_TYPE + tokenResponse.accessToken;
        UserInfo userDetails = idamClient.getUserInfo(tokenWithBearer);
        return new CachedIdamCredential(tokenWithBearer, userDetails.getUid(), Long.valueOf(tokenResponse.expiresIn));
    }

}
