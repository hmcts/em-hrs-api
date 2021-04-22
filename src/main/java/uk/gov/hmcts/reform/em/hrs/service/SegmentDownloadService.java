package uk.gov.hmcts.reform.em.hrs.service;

import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

public interface SegmentDownloadService {

    void download(UUID recordingId, Integer segment, HttpServletResponse response);
}
