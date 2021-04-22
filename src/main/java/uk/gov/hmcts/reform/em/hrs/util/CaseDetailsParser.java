package uk.gov.hmcts.reform.em.hrs.util;

import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import java.util.Map;

public final class CaseDetailsParser {
    private CaseDetailsParser() {
    }

    public static String getShareeEmail(Map<String, Object> data) {
        final String shareeEmailField = "recipientEmailAddress";

        final String shareeEmailAddress = (String) data.get(shareeEmailField);

        if (!EmailValidator.isValid(shareeEmailAddress)) {
            throw new ValidationErrorException(Map.of(shareeEmailField, shareeEmailAddress));
        }

        return shareeEmailAddress;
    }
}
