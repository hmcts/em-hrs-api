package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.em.hrs.exception.EmailNotificationException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.exception.UnauthorisedServiceException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundExceptionsShouldReturnNotFoundStatus() {
        long hearingId = 123L;
        HearingRecordingNotFoundException exception = new HearingRecordingNotFoundException(hearingId);

        ResponseEntity<String> response = handler.handleNotFoundExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleValidationExceptionsShouldReturnBadRequestAndData() {
        Map<String, Object> validationErrors = Map.of("field1", "error1");
        ValidationErrorException exception = new ValidationErrorException(validationErrors);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(validationErrors);
    }

    @Test
    void handleEmailNotificationExceptionShouldReturnInternalServerError() {
        Exception cause = new Exception("Connection failed");
        EmailNotificationException exception = new EmailNotificationException(cause);

        ResponseEntity<String> response = handler.handleEmailNotificationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
            .contains("We have detected a problem and our engineers are working on it");
    }

    @Test
    void handleCatchAllExceptionsShouldReturnInternalServerError() {
        RuntimeException exception = new RuntimeException("Unexpected error");

        ResponseEntity<String> response = handler.handleCatchAllExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
            .contains("We have detected a problem and our engineers are working on it");
    }

    @Test
    void handleInvalidApiKeyExceptionShouldReturnUnauthorized() {
        InvalidApiKeyException exception = new InvalidApiKeyException("Invalid API Key");

        ResponseEntity<String> response = handler.handleInvalidApiKeyException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid API Key");
    }

    @Test
    void handleUnauthorisedServiceExceptionShouldReturnForbidden() {
        UnauthorisedServiceException exception = new UnauthorisedServiceException("Service not allowed");

        ResponseEntity<String> response = handler.handleUnauthorisedServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Service not allowed");
    }
}
