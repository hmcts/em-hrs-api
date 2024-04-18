package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;


class WelcomeControllerTest {

    private final WelcomeController welcomeController = new WelcomeController();

    @Test
    void testEndPointResponseCode() {
        ResponseEntity<Map<String, String>> responseEntity = welcomeController.welcome();

        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void testEndpointResponseMessage() {
        ResponseEntity<Map<String, String>> responseEntity = welcomeController.welcome();

        Map<String, String> expectedResponse = new HashMap<>();
        expectedResponse.put("message", "Welcome to the HRS API!");

        String cacheHeader = responseEntity.getHeaders().getCacheControl();

        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals("no-cache", cacheHeader);
        Assertions.assertEquals(expectedResponse, responseEntity.getBody());
    }
}
