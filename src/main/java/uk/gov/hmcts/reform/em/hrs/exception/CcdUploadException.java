package uk.gov.hmcts.reform.em.hrs.exception;

public class CcdUploadException extends RuntimeException {
    public CcdUploadException(String message) {
        super(message);
    }

    public CcdUploadException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
