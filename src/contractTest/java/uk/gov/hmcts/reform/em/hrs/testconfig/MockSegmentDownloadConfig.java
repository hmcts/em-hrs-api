package uk.gov.hmcts.reform.em.hrs.testconfig;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;

import java.io.IOException;
import java.util.UUID;

@TestConfiguration
public class MockSegmentDownloadConfig {
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
                byte[] dummyData = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10, 0x20, 0x30, 0x40};

                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
                response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                response.setContentLength(dummyData.length);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

                try (ServletOutputStream os = response.getOutputStream()) {
                    os.write(dummyData);
                    os.flush();
                }
            }
        };
    }
}
