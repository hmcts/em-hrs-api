package uk.gov.hmcts.reform.em.hrs.service;

import java.util.Map;


public interface ShareAndNotifyService {

    void shareAndNotify(final Long caseId,
                        final Map<String, Object> caseData,
                        final String authorizationToken);
}
