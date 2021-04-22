package uk.gov.hmcts.reform.em.hrs.exception;

import lombok.NonNull;

import java.util.Map;

public class ValidationErrorException extends BadRequestException {

    public ValidationErrorException(@NonNull final Map<String, Object> data) {
        super(data);
    }

}
