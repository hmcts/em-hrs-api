package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidatorTest {

    @Test
    void testShouldAllowValidEmailAddress() {
        final boolean valid = EmailValidator.isValid("test@emailTest.com");

        assertThat(valid).isTrue();
    }

    @Test
    void testShouldRejectEmptyString() {
        final boolean valid = EmailValidator.isValid("");

        assertThat(valid).isFalse();
    }

    @Test
    void testShouldRejectMalformedEmail() {
        final boolean valid = EmailValidator.isValid("test@emailTest");

        assertThat(valid).isFalse();
    }

    @Test
    void testShouldRejectEmailWithSpecialCharacter() {
        final boolean valid = EmailValidator.isValid("test@emailTest,com");

        assertThat(valid).isFalse();
    }
}
