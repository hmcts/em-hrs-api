package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Map;

public interface SecurityService {
    Map<String, String> getTokens();

    String getUserToken();

    String getUserId();

    String getUserId(String userAuthorization);

    String getUserEmail();

    String getUserEmail(String userAuthorization);

    UserInfo getUserInfo(String jwtToken);

    String getCurrentlyAuthenticatedServiceName();

    String getAuditUserEmail();

    String getClientIp();
}
