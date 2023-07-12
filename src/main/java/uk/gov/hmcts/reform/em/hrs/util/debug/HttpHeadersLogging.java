package uk.gov.hmcts.reform.em.hrs.util.debug;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;

public class HttpHeadersLogging {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeadersLogging.class);

    private HttpHeadersLogging() {
    }

    public static void logHttpHeaders(@NotNull HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                LOGGER.info("HeaderName , Values: {} , {}", headerName, headerValue);
            }
        }

    }
}
