package uk.gov.hmcts.reform.em.hrs.exception;

import lombok.NonNull;

public class HearingRecordingNotFoundException extends ResourceNotFoundException {

    public HearingRecordingNotFoundException(@NonNull Long ccdCaseId) {
        super("CcdCaseId", ccdCaseId.toString());
    }

    @Override
    public String getMessage() {
        return String.format("Hearing Recording with %s: %s is not be found", getField(), getValue());
    }

}
