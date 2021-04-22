package uk.gov.hmcts.reform.em.hrs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class GovNotifyErrorException extends RuntimeException {

    public GovNotifyErrorException(String message) {
        super(message);
    }

    public GovNotifyErrorException(Throwable t) {
        super(t);
    }
}
