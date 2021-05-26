package uk.gov.hmcts.reform.em.hrs.exception;

public class BlobCopyException extends RuntimeException {
    public BlobCopyException(String message) {
        super(message);
    }

    public BlobCopyException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
