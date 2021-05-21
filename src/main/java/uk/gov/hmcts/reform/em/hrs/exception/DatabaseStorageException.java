package uk.gov.hmcts.reform.em.hrs.exception;

public class DatabaseStorageException extends RuntimeException {
    public DatabaseStorageException(String message) {
        super(message);
    }

    public DatabaseStorageException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
