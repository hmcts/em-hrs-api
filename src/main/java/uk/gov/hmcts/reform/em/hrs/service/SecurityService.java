package uk.gov.hmcts.reform.em.hrs.service;

import java.util.Map;

public interface SecurityService {
    Map<String, String> getTokens();

    String getUserToken();

    String getUserId();

    String getUserId(String userAuthorization);

    String getUserEmail();

    String getUserEmail(String userAuthorization);
}
