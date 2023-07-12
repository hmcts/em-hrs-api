package uk.gov.hmcts.reform.em.hrs.util;


import jakarta.servlet.http.HttpServletRequest;

//non frontdoor environments (ie DEMO do not use front door, and may use TitleCase header names)
public class HttpHeaderProcessor {

    private HttpHeaderProcessor() {
    }

    //front door environments use lowercase header names
    public static String getHttpHeaderByCaseSensitiveAndLowerCase(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null) {
            headerValue = request.getHeader(headerName.toLowerCase());
        }
        return headerValue;
    }
}
