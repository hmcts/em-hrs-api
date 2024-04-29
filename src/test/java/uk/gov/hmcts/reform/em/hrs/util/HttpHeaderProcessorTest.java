package uk.gov.hmcts.reform.em.hrs.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class HttpHeaderProcessorTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void testGetHttpHeaderWhenLowerCase() {
        doReturn("header-value").when(request).getHeader("header-name");

        String headerValue = HttpHeaderProcessor
            .getHttpHeaderByCaseSensitiveAndLowerCase(request, "header-name");

        assertEquals("header-value", headerValue);
    }

    @Test
    void testGetHttpHeaderWhenTitleCased() {
        doReturn("header-value").when(request).getHeader("header-name");

        String headerValue = HttpHeaderProcessor
            .getHttpHeaderByCaseSensitiveAndLowerCase(request, "Header-Name");

        assertEquals("header-value", headerValue);
    }
}
