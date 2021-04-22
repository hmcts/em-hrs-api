package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

public interface IngestionService {
    void ingest(HearingRecordingDto hearingRecordingDto);
}
