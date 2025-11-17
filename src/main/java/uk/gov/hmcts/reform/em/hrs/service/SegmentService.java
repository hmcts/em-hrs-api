package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.util.List;
import java.util.UUID;

public interface SegmentService {

    List<HearingRecordingSegment> findByRecordingId(UUID id);

    void createAndSaveSegment(HearingRecording hearingRecording, HearingRecordingDto recordingDto);

}
