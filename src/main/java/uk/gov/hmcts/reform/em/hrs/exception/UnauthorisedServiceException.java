package uk.gov.hmcts.reform.em.hrs.exception;

public class UnauthorisedServiceException extends RuntimeException {
    public UnauthorisedServiceException(String message) {
        super(message);
    }
}
