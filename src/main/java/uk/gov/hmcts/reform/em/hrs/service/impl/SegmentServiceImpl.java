package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentServiceImpl.class);

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobContainerClient blobContainerClient;
    private final Tika tika;
    private final AutoDetectParser parser;

    private static final String VIDEO_MIME = "video/mp4";
    private static final String AUDIO_MIME = "audio/mp4";
    private static final String UNKNOWN_MIME = "application/octet-stream";

    @Autowired
    public SegmentServiceImpl(final HearingRecordingSegmentRepository segmentRepository,
                              @Qualifier("hrsCvpBlobContainerClient") final BlobContainerClient blobContainerClient,
                              final Tika tika, AutoDetectParser parser) {
        this.segmentRepository = segmentRepository;
        this.blobContainerClient = blobContainerClient;
        this.tika = tika;
        this.parser = parser;
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
        String mimeType = detectMimeType(recordingDto.getFilename());

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

    private String detectMimeType(String blobName) {
        try (InputStream inputStream = blobContainerClient.getBlobClient(blobName)
            .openInputStream(new BlobInputStreamOptions().setRange(new BlobRange(0, 2L * 1024 * 1024)))) {

            // First, Try to extract metadata (may give video/audio fields)
            Metadata metadata = getMetadata(inputStream);

            boolean hasVideo = Objects.nonNull(metadata.get("width"))
                    || Objects.nonNull(metadata.get("height"))
                    || Objects.nonNull(metadata.get("video:codec"));

            boolean hasAudio = Objects.nonNull(metadata.get("xmpDM:audioSampleRate"))
                    || Objects.nonNull(metadata.get("xmpDM:audioCompressor"));

            if (hasVideo) {
                return VIDEO_MIME;
            } else if (hasAudio) {
                return AUDIO_MIME;
            } else {
                //This implies parsing via AutoDetectParser has failed to find audio or video metadata
                // Second, detect mime type using Tika
                String mime = tika.detect(inputStream, new Metadata());
                if (Objects.nonNull(mime)) {
                    if (mime.startsWith("video/")) {
                        return VIDEO_MIME;
                    } else if (mime.startsWith("audio/")) {
                        return AUDIO_MIME;
                    }
                    return UNKNOWN_MIME;
                }
                return UNKNOWN_MIME;
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to detect MIME type from blob", e);
        }
    }

    private Metadata getMetadata(InputStream inputStream) {
        Metadata metadata = new Metadata();
        try {
            parser.parse(inputStream, new BodyContentHandler(0), metadata, new ParseContext());
        } catch (Exception e) {
            // parsing metadata failed — fall back to mime only
            LOGGER.error("Failed to parse metadata from blob ", e);
        }
        return metadata;
    }
}
