package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import org.apache.tika.Tika;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.HandlerBox;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobContainerClient blobContainerClient;
    private final Tika tika;

    private static final int HEAD_BYTES = 2 * 1024 * 1024;   // 2MB
    private static final int TAIL_BYTES = 512 * 1024;        // 512KB

    @Autowired
    public SegmentServiceImpl(final HearingRecordingSegmentRepository segmentRepository,
                              @Qualifier("hrsCvpBlobContainerClient") final BlobContainerClient blobContainerClient,
                              final Tika tika) {
        this.segmentRepository = segmentRepository;
        this.blobContainerClient = blobContainerClient;
        this.tika = tika;
    }

    @Override
    public List<HearingRecordingSegment> findByRecordingId(UUID id) {
        return segmentRepository.findByHearingRecordingId(id);
    }

    @Override
    public void createAndSaveSegment(final HearingRecording hearingRecording, final HearingRecordingDto recordingDto) {
        HearingRecordingSegment segment = createSegment(hearingRecording, recordingDto);
        segmentRepository.saveAndFlush(segment);
    }

    private HearingRecordingSegment createSegment(final HearingRecording hearingRecording,
                                                  final HearingRecordingDto recordingDto) {

        boolean videoFile = hasVideoTrack(recordingDto.getFilename());

        String mimeType = videoFile ? "video/mp4" : "audio/mp4";

        return HearingRecordingSegment.builder()
            .filename(recordingDto.getFilename())
            .fileExtension(recordingDto.getFilenameExtension())
            .fileSizeMb(recordingDto.getFileSize())
            .fileMd5Checksum(recordingDto.getCheckSum())
            .ingestionFileSourceUri(recordingDto.getSourceBlobUrl())
            .recordingSegment(recordingDto.getSegment())
            .hearingRecording(hearingRecording)
            .interpreter(recordingDto.getInterpreter())
            .mimeType(mimeType)
            .build();
    }

    /**
     * Returns true if the MP4 blob has at least one video track (hdlr type "vide").
     * This reads a head+tail slice to locate the moov atom when it is at the end.
     */
    public boolean hasVideoTrack(String blobName) {
        try {
            byte[] bytes = fetchHeadAndTailBytes(blobName);
            if (bytes == null || bytes.length == 0) {
                return false;
            }

            try (InputStream in = new ByteArrayInputStream(bytes)) {
                // IsoFile expects a ReadableByteChannel
                IsoFile isoFile = new IsoFile(Channels.newChannel(in));
                List<MovieBox> mboxes = isoFile.getBoxes(MovieBox.class);
                for (MovieBox mb : mboxes) {
                    List<TrackBox> tracks = mb.getBoxes(TrackBox.class);
                    for (TrackBox tb : tracks) {
                        HandlerBox h = tb.getMediaBox().getHandlerBox();
                        if (h != null && "vide".equals(h.getHandlerType())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // swallow/log as appropriate in your app; fall back to safer heuristics
        }
        return false;
    }

    private byte[] fetchHeadAndTailBytes(String blobName) throws Exception {
        var client = blobContainerClient.getBlobClient(blobName);
        long blobSize = -1L;
        try {
            blobSize = client.getProperties().getBlobSize();
        } catch (Exception ignored) {
            blobSize = -1L;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long headLen = (blobSize <= 0) ? HEAD_BYTES : Math.min(blobSize, HEAD_BYTES);
        try (InputStream headStream = client.openInputStream(new BlobInputStreamOptions()
                .setRange(new BlobRange(0, headLen)))) {
            out.write(headStream.readAllBytes());
        }

        if (blobSize > headLen) {
            long tailOffset = Math.max(headLen, blobSize - TAIL_BYTES);
            long tailLen = blobSize - tailOffset;
            try (InputStream tailStream = client.openInputStream(new BlobInputStreamOptions()
                    .setRange(new BlobRange(tailOffset, tailLen)))) {
                out.write(tailStream.readAllBytes());
            } catch (Exception ignored) {
                // tail fetch optional
            }
        }

        return out.toByteArray();
    }

}
