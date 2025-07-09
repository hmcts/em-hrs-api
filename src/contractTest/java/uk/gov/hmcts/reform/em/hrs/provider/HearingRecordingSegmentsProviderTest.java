package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;
import uk.gov.hmcts.reform.em.hrs.service.impl.SegmentDownloadServiceImpl;
import uk.gov.hmcts.reform.em.hrs.storage.BlobInfo;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@Provider("em_hrs_api_recording_segments_provider")
@Import(SegmentDownloadServiceImpl.class)
public class HearingRecordingSegmentsProviderTest extends HearingControllerBaseProviderTest {

    @MockitoBean
    private HearingRecordingSegmentRepository segmentRepository;
    @MockitoBean
    private BlobstoreClient blobstoreClient;
    @MockitoBean
    private AuditEntryService auditEntryService;
    @MockitoBean
    private ShareesRepository shareesRepository;
    @MockitoBean
    private SecurityService securityService;


    @State("A hearing recording segment exists for download")
    public void setupSegmentExists() {
        // Prepare dummy data
        UUID recordingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String filename = "testfile.mp3";
        String hearingSource = "CVP";

        // Dummy HearingRecordingSegment
        HearingRecordingSegment segment = new HearingRecordingSegment();
        segment.setFilename(filename);

        // Dummy HearingRecording
        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setId(recordingId);
        hearingRecording.setHearingSource(hearingSource);
        hearingRecording.setSegments(Collections.singleton(segment));
        segment.setHearingRecording(hearingRecording);
        int segmentNo = 1;

        // Mock segmentRepository
        doReturn(segment)
            .when(segmentRepository)
            .findByHearingRecordingIdAndRecordingSegment(ArgumentMatchers.eq(recordingId),
                                                         ArgumentMatchers.eq(segmentNo));

        // Mock blobstoreClient
        String contentType = "text/plain";
        long fileSize = 1024L;
        BlobInfo blobInfo = new BlobInfo(fileSize,contentType);
        doReturn(blobInfo).when(blobstoreClient).fetchBlobInfo(
            ArgumentMatchers.eq(filename),
            ArgumentMatchers.eq(hearingSource)
        );

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(2); // Use OutputStream, not ServletOutputStream directly
            byte[] data = new byte[1024];
            Arrays.fill(data, (byte) 'i'); // Fill with ASCII 'i', 1024 times
            out.write(data);
            out.flush(); // Ensure everything is sent
            return null;
        }).when(blobstoreClient).downloadFile(
            ArgumentMatchers.eq(filename),
            ArgumentMatchers.any(), // blobRange
            ArgumentMatchers.any(OutputStream.class),
            ArgumentMatchers.eq(hearingSource)
        );
    }

    @State("A hearing recording file exists for download by file name")
    public void setupFileExists() {
        // Prepare dummy data
        UUID recordingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String fileName = "testfile.mp3";
        String folderName = "folderA";
        String fileNameDecoded = folderName + java.io.File.separator + fileName;

        // Create dummy HearingRecording
        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setId(recordingId);
        hearingRecording.setHearingSource("CVP");

        // Create dummy HearingRecordingSegment and link it
        HearingRecordingSegment segment = new HearingRecordingSegment();
        segment.setFilename(fileNameDecoded);
        segment.setHearingRecording(hearingRecording);

        // Mock segmentRepository for both endpoints
        doReturn(segment)
            .when(segmentRepository)
            .findByHearingRecordingIdAndFilename(
                ArgumentMatchers.any(UUID.class),
                ArgumentMatchers.any(String.class)
            );

        // Mock blobstoreClient
        String hearingSource = "CVP";
        String contentType = "text/plain";
        long fileSize = 1024L;
        BlobInfo blobInfo = new BlobInfo(fileSize, contentType);
        doReturn(blobInfo).when(blobstoreClient).fetchBlobInfo(ArgumentMatchers.any(String.class),
                                                               ArgumentMatchers.any(String.class)
        );

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(2);
            byte[] data = new byte[1024];
            Arrays.fill(data, (byte) 'f');
            out.write(data);
            out.flush();
            return null;
        }).when(blobstoreClient).downloadFile(ArgumentMatchers.eq(fileNameDecoded),
                                              ArgumentMatchers.any(),
                                              ArgumentMatchers.any(OutputStream.class),
                                              ArgumentMatchers.eq(hearingSource)
        );
    }
}
