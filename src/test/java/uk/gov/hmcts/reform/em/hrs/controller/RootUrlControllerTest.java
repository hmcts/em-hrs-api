package uk.gov.hmcts.reform.em.hrs.controller;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RootUrlControllerTest {

    final RootUrlController rootUrlController = new RootUrlController();

    @Test
    void test_should_return_welcome_response() {

        ResponseEntity<String> responseEntity = rootUrlController.welcome();
        String expectedMessage = "Welcome to Hearing Recordings Service";

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody()).contains(expectedMessage);
    }
}
