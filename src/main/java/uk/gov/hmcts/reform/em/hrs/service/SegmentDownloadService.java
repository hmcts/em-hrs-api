package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SegmentDownloadService {

    HearingRecordingSegment fetchSegmentByRecordingIdAndSegmentNumber(UUID recordingId, Integer segmentNo,
                                                                      String userToken);


    void download(HearingRecordingSegment segment, HttpServletRequest request,
                  HttpServletResponse response) throws IOException;
}
