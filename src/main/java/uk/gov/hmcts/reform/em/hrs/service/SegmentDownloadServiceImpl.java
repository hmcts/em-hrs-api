package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.storage.blob.models.BlobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

@Service
public class SegmentDownloadServiceImpl implements SegmentDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentDownloadServiceImpl.class);

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobstoreClient blobstoreClient;
    private AuditEntryService auditEntryService;

    @Autowired
    public SegmentDownloadServiceImpl(HearingRecordingSegmentRepository segmentRepository,
                                      BlobstoreClient blobstoreClient, AuditEntryService auditEntryService) {
        this.segmentRepository = segmentRepository;
        this.blobstoreClient = blobstoreClient;
        this.auditEntryService = auditEntryService;
    }

    @Override
    @PreAuthorize("hasPermission(#recordingId,'READ')")
    public Map<String, String> getDownloadInfo(UUID recordingId, Integer segmentNo) {

        HearingRecordingSegment segment =
            segmentRepository.findByHearingRecordingIdAndRecordingSegment(recordingId, segmentNo);

        auditEntryService.createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);

        BlobProperties blobProperties = blobstoreClient.getBlobProperties(segment.getFilename());

        LOGGER.info(
            "downloading blob with the following properties: [filenmae: {}, content-type: {}, content-length: {}]",
            segment.getFilename(), blobProperties.getContentType(), blobProperties.getBlobSize()
        );

        return Map.of(
            "filename", segment.getFilename(),
            "contentType", blobProperties.getContentType(),
            "contentLength", String.valueOf(blobProperties.getBlobSize())
        );
    }

    @Override
    @PreAuthorize("hasPermission(#recordingId,'READ')")
    public void download(String filename, OutputStream responseOutputStream) {

        blobstoreClient.downloadFile(filename, responseOutputStream);

        HearingRecordingSegment segment = segmentRepository.findByFilename(filename);

        auditEntryService.createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
    }
}
