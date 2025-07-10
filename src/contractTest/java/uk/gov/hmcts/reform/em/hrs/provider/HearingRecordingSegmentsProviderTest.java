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

    private static final String FILE_NAME = "testfile.mp3";
    private static final String HEARING_SOURCE = "CVP";
    private static final UUID RECORDING_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private void mockBlobClient(char fillingChar) {
        // Mock blobstoreClient
        String contentType = "text/plain";
        long fileSize = 1024L;
        BlobInfo blobInfo = new BlobInfo(fileSize, contentType);
        doReturn(blobInfo)
            .when(blobstoreClient)
            .fetchBlobInfo(ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class));

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(2);
            byte[] data = new byte[1024];
            Arrays.fill(data, (byte) fillingChar);
            out.write(data);
            out.flush(); // Ensure everything is sent
            return null;
        }).when(blobstoreClient).downloadFile(
            ArgumentMatchers.any(String.class),
            ArgumentMatchers.any(), // blobRange
            ArgumentMatchers.any(OutputStream.class),
            ArgumentMatchers.any(String.class)
        );
    }

    @State("A hearing recording segment exists for download")
    public void setupSegmentExists() {

        // Dummy HearingRecordingSegment
        HearingRecordingSegment segment = new HearingRecordingSegment();
        segment.setFilename(FILE_NAME);

        // Dummy HearingRecording
        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setId(RECORDING_ID);
        hearingRecording.setHearingSource(HEARING_SOURCE);
        hearingRecording.setSegments(Collections.singleton(segment));
        segment.setHearingRecording(hearingRecording);
        int segmentNo = 1;

        // Mock segmentRepository
        doReturn(segment)
            .when(segmentRepository)
            .findByHearingRecordingIdAndRecordingSegment(ArgumentMatchers.eq(RECORDING_ID),
                                                         ArgumentMatchers.eq(segmentNo));

        // Mock blobstoreClient
        mockBlobClient('i');
    }

    @State({"A hearing recording file exists for download by file name",
        "A hearing recording file exists for download by folder and file name"})
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
