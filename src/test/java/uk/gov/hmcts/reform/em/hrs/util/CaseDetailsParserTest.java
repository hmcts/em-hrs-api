package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class CaseDetailsParserTest {

    @Test
    void testShouldReturnShareeEmail() {
        String shareeMail = "sharee.tester@test.com";
        final Map<String, Object> data = Map.of("recipientEmailAddress", shareeMail);

        final String shareeEmail = CaseDetailsParser.getShareeEmail(data);

        assertThat(shareeEmail).isNotNull().isEqualTo(shareeMail);
    }

    @Test
    void testShouldRaiseValidationException() {
        final Map<String, Object> data = Map.of("recipientEmailAddress", "aaaaa@email");

        assertThatExceptionOfType(ValidationErrorException.class)
            .isThrownBy(() -> CaseDetailsParser.getShareeEmail(data));
    }

}
