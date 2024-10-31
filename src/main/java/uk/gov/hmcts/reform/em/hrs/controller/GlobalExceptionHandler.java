package uk.gov.hmcts.reform.em.hrs.controller;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.em.hrs.exception.EmailNotificationException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.exception.UnauthorisedServiceException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = getLogger(GlobalExceptionHandler.class);

    private static final String SERVER_ERROR_MESSAGE = "We have detected a problem and our engineers are working on it."
        + "\nPlease try again later and thank you for your patience";

    @ExceptionHandler(HearingRecordingNotFoundException.class)
    public final ResponseEntity<String> handleNotFoundExceptions(HearingRecordingNotFoundException e) {
        log(HttpStatus.NOT_FOUND, e);

        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationErrorException.class)
    public final ResponseEntity<Map<String, Object>> handleValidationExceptions(ValidationErrorException e) {
        log(HttpStatus.BAD_REQUEST, e);

        return new ResponseEntity<>(e.getData(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailNotificationException.class)
    public final ResponseEntity<String> handleEmailNotificationException(EmailNotificationException e) {
        log(HttpStatus.INTERNAL_SERVER_ERROR, e);

        return new ResponseEntity<>(SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<String> handleCatchAllExceptions(Throwable e) {
        log(HttpStatus.INTERNAL_SERVER_ERROR, e);

        return new ResponseEntity<>(SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(InvalidApiKeyException.class)
    public final ResponseEntity<String> handleInvalidApiKeyException(InvalidApiKeyException e) {
        log(HttpStatus.UNAUTHORIZED, e);

        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorisedServiceException.class)
    public final ResponseEntity<String> handleUnauthorisedServiceException(UnauthorisedServiceException e) {
        log(HttpStatus.FORBIDDEN, e);

        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    private void log(final HttpStatus status, final Throwable throwable) {
        LOGGER.error("Responding with status: {}, error encountered ====>\n{}", status, throwable);
    }

}
