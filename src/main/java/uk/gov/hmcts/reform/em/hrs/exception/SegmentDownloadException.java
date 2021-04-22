package uk.gov.hmcts.reform.em.hrs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class SegmentDownloadException extends RuntimeException {

    public SegmentDownloadException(String message) {
        super(message);
    }

    public SegmentDownloadException(Throwable t) {
        super(t);
    }
}
