package uk.gov.hmcts.reform.em.hrs.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class HttpHeaderProcessorTest {

    private HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void testGetHttpHeaderWhenLowerCase() {
        doReturn("header-value").when(request).getHeader("header-name");

        String headerValue = HttpHeaderProcessor
            .getHttpHeaderByCaseSensitiveAndLowerCase(request, "header-name");

        assertTrue(headerValue.equals("header-value"));
    }

    @Test
    void testGetHttpHeaderWhenTitleCased() {
        doReturn("header-value").when(request).getHeader("header-name");

        String headerValue = HttpHeaderProcessor
            .getHttpHeaderByCaseSensitiveAndLowerCase(request, "Header-Name");

        assertTrue(headerValue.equals("header-value"));
    }
}
