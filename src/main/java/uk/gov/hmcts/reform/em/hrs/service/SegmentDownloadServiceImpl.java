package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.OutputStream;
import java.util.UUID;

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
    public void download(UUID recordingId, Integer segmentNo, OutputStream outputStream) {

        HearingRecordingSegment segment = segmentRepository
            .findByHearingRecordingIdAndRecordingSegment(recordingId, segmentNo);
        blobstoreClient.downloadFile(segment.getFilename(), outputStream);
    }
}
