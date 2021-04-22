package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.exception.SegmentDownloadException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

@Service
public class SegmentDownloadServiceImpl implements SegmentDownloadService {

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobstoreClient blobstoreClient;

    @Autowired
    public SegmentDownloadServiceImpl(HearingRecordingSegmentRepository segmentRepository,
                                      BlobstoreClient blobstoreClient) {
        this.segmentRepository = segmentRepository;
        this.blobstoreClient = blobstoreClient;
    }

    @Override
    public void download(UUID recordingId, Integer segmentNo, HttpServletResponse response) {

        HearingRecordingSegment segment =
            segmentRepository.findByHearingRecordingIdAndRecordingSegment(recordingId, segmentNo);

        response.setHeader("Content-Disposition",String.format("attachment; filename=%s", segment.getFilename()));
        try {
            blobstoreClient.downloadFile(segment.getFilename(), response.getOutputStream());
        } catch (IOException e) {
            throw new SegmentDownloadException(e);
        }
    }
}
