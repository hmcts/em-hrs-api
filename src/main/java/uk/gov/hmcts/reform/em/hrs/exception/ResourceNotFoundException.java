package uk.gov.hmcts.reform.em.hrs.exception;

import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class ResourceNotFoundException extends RuntimeException {

    private final String field;
    private final String value;

    protected ResourceNotFoundException(@NonNull final String field, @NonNull final String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public String getMessage() {
        return String.format("Resource with %s: %s could not be found", field, value);
    }

}
