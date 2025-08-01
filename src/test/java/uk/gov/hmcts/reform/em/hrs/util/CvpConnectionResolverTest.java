package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.extractAccountFromUrl;

class CvpConnectionResolverTest {

    @Test
    void testUrlWithSecondaryDnsWorks() {
        String input = "https://cvprecordingsstgsa-secondary.blob.core.windows.net/";
        String expected = "cvprecordingsstgsa";
        String actual = extractAccountFromUrl(input);
        assertEquals(expected, actual);
    }

    @Test
    void testUrlWithPrimaryDnsWorks() {
        String input = "https://cvprecordingsstgsa.blob.core.windows.net/";
        String expected = "cvprecordingsstgsa";
        String actual = extractAccountFromUrl(input);
        assertEquals(expected, actual);

    }

    @Test
    void testThatInvalidUrlReturnsNull() {
        String input = "this is an invalid url";
        String actual = extractAccountFromUrl(input);
        assertNull(actual);
    }
}
