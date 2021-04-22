package uk.gov.hmcts.reform.em.hrs.util;

import java.util.regex.Pattern;

public final class EmailValidator {

    private EmailValidator() {
    }

    public static boolean isValid(final String emailAddress) {
        return Pattern.matches("^\\S+@\\S+\\.\\S+$", emailAddress);
    }
}
