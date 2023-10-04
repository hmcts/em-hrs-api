package uk.gov.hmcts.reform.em.hrs.exception;

import lombok.NonNull;

public class BlobNotFoundException extends ResourceNotFoundException {


    public BlobNotFoundException(@NonNull String field, @NonNull String value) {
        super(field, value);
    }

}
