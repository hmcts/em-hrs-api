package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HearingRecordingService {

    void deleteCaseHearingRecordings(Collection<Long> ccdCaseIds);

    Long findCcdCaseIdByFilename(String filename);

    Optional<HearingRecording> findHearingRecording(HearingRecordingDto recordingDto);

    HearingRecording createHearingRecording(HearingRecordingDto recordingDto);

    HearingRecording updateCcdCaseId(HearingRecording recording, Long ccdCaseId);

    List<HearingRecordingTtlMigrationDTO> getRecordingsForTtlUpdate(int limit);

    void updateTtl(UUID recordingId, LocalDate ttl);
}
