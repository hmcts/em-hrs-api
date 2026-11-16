package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.service.CaseAccessCategoryService;

@Service
public class CaseAccessCategoryServiceImpl implements CaseAccessCategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAccessCategoryServiceImpl.class);
    private static final String PREFIX = "HRS:";
    private static final String DEFAULT_CATEGORY = "HRS:AA";

    @Override
    public String getCaseAccessCategory(String serviceId) {
        if (serviceId == null || serviceId.length() < 2) {
            LOGGER.warn(
                "serviceId is null or too short to derive caseAccessCategory, using default: {}",
                DEFAULT_CATEGORY
            );
            return DEFAULT_CATEGORY;
        }
        return PREFIX + serviceId.substring(0, 2).toUpperCase();
    }

}
