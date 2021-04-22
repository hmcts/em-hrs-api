package uk.gov.hmcts.reform.em.hrs.exception;

public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(Throwable e) {
        super(e);
    }
}
