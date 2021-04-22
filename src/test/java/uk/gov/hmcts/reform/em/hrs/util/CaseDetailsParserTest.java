package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;

class CaseDetailsParserTest {

    @Test
    void testShouldReturnShareeEmail() {
        final Map<String, Object> data = Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS);

        final String shareeEmail = CaseDetailsParser.getShareeEmail(data);

        assertThat(shareeEmail).isNotNull().isEqualTo(SHAREE_EMAIL_ADDRESS);
    }

    @Test
    void testShouldRaiseValidationException() {
        final Map<String, Object> data = Map.of("recipientEmailAddress", "aaaaa@email");

        assertThatExceptionOfType(ValidationErrorException.class)
            .isThrownBy(() -> CaseDetailsParser.getShareeEmail(data));
    }

}
