package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Provider("em_hrs_api_recording_download_sharee_provider")
@Import(HearingRecordingDownloadShareeProviderTest.MockSegmentDownloadConfig.class)
public class HearingRecordingDownloadShareeProviderTest extends HearingControllerBaseProviderTest {

    @MockitoBean
    private SegmentDownloadService segmentDownloadService;

    @State("A segment exists for recording ID and segment number for download")
    public void setupValidSegmentDownload() throws IOException {

    }


    @TestConfiguration
    public static class MockSegmentDownloadConfig {

        @Bean
        public SegmentDownloadService segmentDownloadService() {
            return new SegmentDownloadService() {

                @Override
                public HearingRecordingSegment fetchSegmentByRecordingIdAndSegmentNumber(
                    UUID recordingId, Integer segmentNo, String userToken, boolean isSharee) {

                    HearingRecordingSegment segment = new HearingRecordingSegment();
                    segment.setFilename("mocked-file.mp3");

                    HearingRecording recording = new HearingRecording();
                    recording.setHearingSource("MOCK_SOURCE");

                    segment.setHearingRecording(recording);
                    return segment;
                }

                @Override
                public HearingRecordingSegment fetchSegmentByRecordingIdAndFileNameForSharee(
                    UUID recordingId,
                    String fileName,
                    String userToken
                ) {
                    return null;
                }

                @Override
                public HearingRecordingSegment fetchSegmentByRecordingIdAndFileName(
                    UUID recordingId,
                    String fileName
                ) {
                    return null;
                }

                public void download(HearingRecordingSegment segment,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {

                    String filename = segment.getFilename();
                    byte[] dummyData = "binary-mock-data".getBytes(StandardCharsets.UTF_8); // or any binary content

                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
                    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                    response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                    response.setContentLength(dummyData.length);

                    try (ServletOutputStream os = response.getOutputStream()) {
                        os.write(dummyData);
                        os.flush();
                    }
                }
            };
        }
    }
}
