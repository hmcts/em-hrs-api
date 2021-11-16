package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;

public interface JobInProgressService {
    void register(HearingRecordingDto hearingRecordingDto) throws DatabaseStorageException;
    void deRegister(HearingRecordingDto hearingRecordingDto);
}
