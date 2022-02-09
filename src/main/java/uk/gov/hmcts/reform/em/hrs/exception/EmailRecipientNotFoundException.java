package uk.gov.hmcts.reform.em.hrs.exception;

public class EmailRecipientNotFoundException extends RuntimeException {

    public EmailRecipientNotFoundException(String message) {
        super(message);
    }
}
