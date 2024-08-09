package uk.gov.hmcts.reform.em.hrs.service;

import java.util.Collection;

public interface HearingRecordingService {

    long deleteCaseHearingRecordings(Collection<Long> ccdCaseIds);
}
