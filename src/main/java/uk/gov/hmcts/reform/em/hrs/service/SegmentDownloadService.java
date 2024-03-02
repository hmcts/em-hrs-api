package uk.gov.hmcts.reform.em.hrs.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.io.IOException;
import java.util.UUID;

public interface SegmentDownloadService {

    HearingRecordingSegment fetchSegmentByRecordingIdAndSegmentNumber(UUID recordingId, Integer segmentNo,
                                                                      String userToken, boolean isSharee);


    HearingRecordingSegment fetchSegmentByRecordingIdAndSegmentName(UUID recordingId, String segmentName,
                                                                      String userToken, boolean isSharee);

    void download(HearingRecordingSegment segment, HttpServletRequest request,
                  HttpServletResponse response) throws IOException;
}
