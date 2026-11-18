package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseAccessCategoryServiceImplTest {

    private final CaseAccessCategoryServiceImpl underTest = new CaseAccessCategoryServiceImpl();

    @Test
    void shouldReturnCategoryWithFirstTwoCharsOfServiceId() {
        assertEquals("HRS:AA", underTest.getCaseAccessCategory("AAA1"));
    }

    @Test
    void shouldReturnCategoryUsingFirstTwoCharsOnly() {
        assertEquals("HRS:AB", underTest.getCaseAccessCategory("ABA1"));
    }

    @Test
    void shouldReturnCategoryWithDifferentPrefix() {
        assertEquals("HRS:BA", underTest.getCaseAccessCategory("BAA2"));
    }

    @Test
    void shouldUppercaseServiceId() {
        assertEquals("HRS:AA", underTest.getCaseAccessCategory("aaa1"));
    }

    @Test
    void shouldWorkWithExactlyTwoCharServiceId() {
        assertEquals("HRS:AB", underTest.getCaseAccessCategory("AB"));
    }

    @Test
    void shouldThrowValidationErrorExceptionWhenServiceIdIsNull() {
        assertThrows(ValidationErrorException.class, () -> underTest.getCaseAccessCategory(null));
    }

    @Test
    void shouldThrowValidationErrorExceptionWhenServiceIdIsSingleChar() {
        assertThrows(ValidationErrorException.class, () -> underTest.getCaseAccessCategory("A"));
    }

    @Test
    void shouldThrowValidationErrorExceptionWhenServiceIdIsEmpty() {
        assertThrows(ValidationErrorException.class, () -> underTest.getCaseAccessCategory(""));
    }

}
