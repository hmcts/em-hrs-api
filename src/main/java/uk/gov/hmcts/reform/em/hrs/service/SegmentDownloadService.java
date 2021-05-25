package uk.gov.hmcts.reform.em.hrs.service;

import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

public interface SegmentDownloadService {

    Map<String, String> getDownloadInfo(UUID recordingId, Integer segmentNo);

    void download(String filename, OutputStream responseOutputStream);
}
