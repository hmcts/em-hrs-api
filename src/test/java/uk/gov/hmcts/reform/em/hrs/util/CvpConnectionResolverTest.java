package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver.isACvpEndpointUrl;

public class CvpConnectionResolverTest {
    @Test
    public void testIsACvpEndpointUrl() {
        //SECURITY NOTICE The below connection string / account key contains no sensitive information, and is derived
        //from the publicly known developer key - https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string
        assertFalse(isACvpEndpointUrl(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=ACTUALKEYNOTNEEDEDFORTHISTEST"
                + "BlobEndpoint=http://localhost:10000/devstoreaccount1"));
        assertFalse(isACvpEndpointUrl(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=ACTUALKEYNOTNEEDEDFORTHISTEST"
                + "BlobEndpoint=https://cvprecordingssboxsa.blob.core.windows.net/"));
        assertFalse(isACvpEndpointUrl("https://hrsendpoint.blob.core.windows.net/"));

        assertTrue(isACvpEndpointUrl("https://cvprecordingssboxsa.blob.core.windows.net/"));
        assertTrue(isACvpEndpointUrl("https://cvprecordingsstgsa.blob.core.windows.net/"));
        assertTrue(isACvpEndpointUrl("https://cvprecordingsprodsa.blob.core.windows.net/"));
    }
}

