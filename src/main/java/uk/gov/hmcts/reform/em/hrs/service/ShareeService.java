package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;

public interface ShareeService {
    HearingRecordingSharee createAndSaveEntry(String emailAddress, HearingRecording hearingRecording);
}
