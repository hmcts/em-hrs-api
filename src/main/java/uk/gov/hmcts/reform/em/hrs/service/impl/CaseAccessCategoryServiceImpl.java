package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.service.CaseAccessCategoryService;

import java.util.Map;

@Service
public class CaseAccessCategoryServiceImpl implements CaseAccessCategoryService {

    private static final String PREFIX = "HRS:";

    @Override
    public String getCaseAccessCategory(String serviceId) {
        if (serviceId == null || serviceId.length() < 2) {
            throw new ValidationErrorException(
                Map.of("serviceId", "serviceId must be at least 2 characters to derive caseAccessCategory")
            );
        }
        return PREFIX + serviceId.substring(0, 2).toUpperCase();
    }

}
