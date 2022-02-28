package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.storage.blob.models.BlobRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidRangeRequestException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobInfo;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;
import uk.gov.hmcts.reform.em.hrs.util.HttpHeaderProcessor;
import uk.gov.hmcts.reform.em.hrs.util.debug.HttpHeadersLogging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class SegmentDownloadServiceImpl implements SegmentDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentDownloadServiceImpl.class);
    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.
    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobstoreClient blobstoreClient;
    private final AuditEntryService auditEntryService;
    private final ShareesRepository shareesRepository;
    private final SecurityService securityService;


    @Value("${shareelink.ttl}")
    private final int validityInHours;

    @Autowired
    public SegmentDownloadServiceImpl(HearingRecordingSegmentRepository segmentRepository,
                                      BlobstoreClient blobstoreClient, AuditEntryService auditEntryService,
                                      ShareesRepository shareesRepository,
                                      SecurityService securityService,
                                      @Value("${shareelink.ttl}") int validityInHours) {
        this.segmentRepository = segmentRepository;
        this.blobstoreClient = blobstoreClient;
        this.auditEntryService = auditEntryService;
        this.shareesRepository = shareesRepository;
        this.securityService = securityService;
        this.validityInHours = validityInHours;
    }


    @Override
    public HearingRecordingSegment fetchSegmentByRecordingIdAndSegmentNumber(UUID recordingId, Integer segmentNo,
                                                                             String userToken) {

        //Check if user access has expired
        String userEmail = securityService.getUserEmail(userToken);
        List<HearingRecordingSharee> hearingRecordingSharees = shareesRepository.findByShareeEmail(userEmail);
        LOGGER.debug("User  {} is trying to access the recordingId  {} with segment Number {}",
                     userEmail, recordingId, segmentNo);
        if (!isEmpty(hearingRecordingSharees)) {
            LOGGER.debug("User  {} has shared recordings", userEmail);
            Optional<HearingRecordingSharee> recordingSharee = hearingRecordingSharees.stream()
                .filter(hearingRecordingSharee ->
                            getHearingRecordingShareeSegment(hearingRecordingSharee.getHearingRecording(), segmentNo))
                .filter(hearingRecordingSharee -> isAccessValid(hearingRecordingSharee.getSharedOn(), userEmail))
                .findAny();
            if (recordingSharee.isEmpty()) {
                throw new ValidationErrorException(Map.of("error", Constants.SHARED_EXPIRED_LINK_MSG));
            }
        }

        HearingRecordingSegment segment =
            segmentRepository.findByHearingRecordingIdAndRecordingSegment(recordingId, segmentNo);

        return segment;
    }

    @Override
    @PreAuthorize("hasPermission(#segment,'READ')")
    public void download(HearingRecordingSegment segment, HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        auditEntryService.createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);

        String filename = segment.getFilename();
        BlobInfo blobInfo = blobstoreClient.fetchBlobInfo(filename);
        long fileSize = blobInfo.getFileSize();
        String contentType = blobInfo.getContentType();
        String attachmentFilename = String.format("attachment; filename=%s", filename);

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, attachmentFilename);
        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setBufferSize(DEFAULT_BUFFER_SIZE);

        HttpHeadersLogging
            .logHttpHeaders(request);//keep during early life support to assist with any range or other issues.

        String rangeHeader = HttpHeaderProcessor.getHttpHeaderByCaseSensitiveAndLowerCase(request, HttpHeaders.RANGE);
        LOGGER.info("Range header for filename {} = {}", filename, rangeHeader);

        BlobRange blobRange = null;
        if (rangeHeader == null) {
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
        } else {
            try {
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());

                // Range headers can request a multipart range but this is not to be supported yet
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
                // Will only accept 1 range first requested range and process it
                List<HttpRange> byteRanges = HttpRange.parseRanges(rangeHeader);
                HttpRange firstByteRange = byteRanges.get(0);

                long byteRangeStart = firstByteRange.getRangeStart(fileSize);
                long byteRangeEnd = firstByteRange.getRangeEnd(fileSize);
                long byteRangeCount = (byteRangeEnd - byteRangeStart) + 1;

                blobRange = new BlobRange(byteRangeStart, byteRangeCount);

                String contentRangeResponse = "bytes " + byteRangeStart + "-" + byteRangeEnd + "/" + fileSize;

                response.setHeader(HttpHeaders.CONTENT_RANGE, contentRangeResponse);
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(byteRangeCount));

                LOGGER.debug(
                    "Calc Blob Values: blobStart {}, blobLength {}",
                    blobRange.getOffset(),
                    blobRange.getCount()
                );
                LOGGER.debug(
                    "Calc Http Header Values: CONTENT_RANGE {}, CONTENT_LENGTH {}",
                    request.getHeader(HttpHeaders.CONTENT_RANGE),
                    request.getHeader(HttpHeaders.CONTENT_LENGTH)
                );
            } catch (Exception e) {
                auditEntryService.createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_FAIL);
                throw new InvalidRangeRequestException(response, fileSize);
            }
        }
        ServletOutputStream outputStream = response.getOutputStream();
        blobstoreClient.downloadFile(filename, blobRange, outputStream);
        auditEntryService.createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
    }

    private boolean isAccessValid(LocalDateTime sharedOn, String userEmail) {
        LocalDateTime expiryTime = sharedOn.plusHours(validityInHours);
        LocalDateTime presentTime = LocalDateTime.now();

        LOGGER.debug("sharedOn value is  {} with expiryTime as {} and presentTime as {} resulted in {} for email {}",
                     sharedOn, expiryTime, presentTime, presentTime.isBefore(expiryTime), userEmail);
        return  presentTime.isBefore(expiryTime);
    }

    private boolean getHearingRecordingShareeSegment(HearingRecording hearingRecording,
                                                     Integer segmentNo) {
        //Need to check if the segment is associated with this Sharee.
        boolean segmentMatch = hearingRecording.getSegments()
            .stream()
            .filter(segment -> segment.getRecordingSegment().equals(segmentNo))
            .findAny()
            .isEmpty();
        LOGGER.debug("Segment Match for segment number {} for hearingRecording with {} was {} ",
                     segmentNo, hearingRecording.getId(), !segmentMatch);
        return !segmentMatch;
    }
}
