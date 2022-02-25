package uk.gov.hmcts.reform.em.hrs.service.email;

public class SendEmailException extends Exception {
    public SendEmailException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
