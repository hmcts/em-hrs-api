package uk.gov.hmcts.reform.em.hrs.service.ccd;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

public interface CcdUploadService {
    void upload(HearingRecordingDto hearingRecordingDto);
}
