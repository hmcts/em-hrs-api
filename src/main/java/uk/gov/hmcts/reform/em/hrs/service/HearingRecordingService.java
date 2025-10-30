package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.util.Collection;
import java.util.Optional;

public interface HearingRecordingService {

    void deleteCaseHearingRecordings(Collection<Long> ccdCaseIds);

    Long findCcdCaseIdByFilename(String filename);

    Optional<HearingRecording> findHearingRecording(HearingRecordingDto recordingDto);

    HearingRecording createHearingRecording(HearingRecordingDto recordingDto);

    HearingRecording updateCcdCaseId(HearingRecording recording, Long ccdCaseId);
}
