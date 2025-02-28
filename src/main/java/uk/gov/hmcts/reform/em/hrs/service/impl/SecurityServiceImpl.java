package uk.gov.hmcts.reform.em.hrs.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;
import uk.gov.hmcts.reform.em.hrs.service.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.em.hrs.service.idam.cache.IdamCachedClient;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Map;
import java.util.Objects;

@Component
public class SecurityServiceImpl implements SecurityService {

    static final String DUMMY_NAME = "dummyName";
    static final String SERVICE_AUTH = "serviceauthorization";
    static final String USER_AUTH = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HRS_INGESTOR = "hrsIngestor";
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceImpl.class);
    public static final String CLIENTIP = "x-azure-clientip";
    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final AuthTokenValidator authTokenValidator;
    private final IdamCachedClient idamCachedClient;

    @Autowired
    public SecurityServiceImpl(
        final IdamClient idamClient,
        final AuthTokenGenerator authTokenGenerator,
        final AuthTokenValidator authTokenValidator,
        final IdamCachedClient idamCachedClient
    ) {
        this.idamClient = idamClient;
        this.authTokenGenerator = authTokenGenerator;
        this.authTokenValidator = authTokenValidator;
        this.idamCachedClient = idamCachedClient;
    }

    @Override
    public Map<String, String> createTokens() {
        final CachedIdamCredential token = createSystemToken();
        return Map.of("user", token.accessToken,
                      "userId", token.userId,
                      "service", authTokenGenerator.generate());
    }

    private CachedIdamCredential createSystemToken() {
        return idamCachedClient.getIdamCredentials();
    }


    @Override
    public String getUserEmail(String userAuthorization) {
        return idamClient.getUserInfo(userAuthorization).getSub();
    }

    @Override
    public UserInfo getUserInfo(String jwtToken) {
        return idamClient.getUserInfo(jwtToken);
    }

    private String getServiceName(final String token) {
        String formattedToken = token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX + token;
        return authTokenValidator.getServiceName(formattedToken);
    }

    @Override
    public String getCurrentlyAuthenticatedServiceName() {

        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            LOGGER.warn("Using dummy serviceName because request is null");
            return DUMMY_NAME;
        }
        String s2sToken = request.getHeader(SERVICE_AUTH);
        if (StringUtils.isBlank(s2sToken)) {
            LOGGER.warn("Using dummy serviceName because token is blank");
            return DUMMY_NAME;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Using token ({}) to get serviceName", s2sToken.substring(0, 15));
        }
        return getServiceName(s2sToken);
    }

    @Override
    public String getAuditUserEmail() {
        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            return HRS_INGESTOR;
        }
        String jwt = request.getHeader(USER_AUTH);

        return getUserEmail(jwt);
    }

    @Override
    public String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            return null;
        }
        return request.getHeader(CLIENTIP);
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

}
