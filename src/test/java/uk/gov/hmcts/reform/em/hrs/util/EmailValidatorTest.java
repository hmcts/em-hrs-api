package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidatorTest {

    @Test
    void testShouldAllowValidEmailAddress() {
        final boolean valid = EmailValidator.isValid("test@emailTest.com");

        assertThat(valid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "test@emailTest", "test@emailTest,com"})
    void testShouldRejectInvalidEmailAddresses(String invalidEmail) {
        final boolean valid = EmailValidator.isValid(invalidEmail);

        assertThat(valid).isFalse();
    }
}
