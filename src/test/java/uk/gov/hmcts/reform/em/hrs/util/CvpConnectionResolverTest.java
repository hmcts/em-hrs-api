package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.extractAccountFromUrl;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.isACvpEndpointUrl;

public class CvpConnectionResolverTest {
    @Test
    public void testIsACvpEndpointUrl() {

        assertFalse(isACvpEndpointUrl(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=ACTUALKEYNOTNEEDEDFORTHISTEST"
                + "BlobEndpoint=http://localhost:10000/devstoreaccount1"));
        assertFalse(isACvpEndpointUrl(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=ACTUALKEYNOTNEEDEDFORTHISTEST"
                + "BlobEndpoint=https://cvprecordingssboxsa.blob.core.windows.net/"));
        assertTrue(isACvpEndpointUrl("https://hrsendpoint.blob.core.windows.net/"));

        assertTrue(isACvpEndpointUrl("https://cvprecordingssboxsa.blob.core.windows.net/"));
        assertTrue(isACvpEndpointUrl("https://cvprecordingsstgsa.blob.core.windows.net/"));
        assertTrue(isACvpEndpointUrl("https://cvprecordingsprodsa.blob.core.windows.net/"));
    }

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
}

