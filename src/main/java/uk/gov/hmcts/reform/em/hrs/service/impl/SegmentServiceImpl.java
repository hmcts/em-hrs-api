package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import org.apache.tika.Tika;
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
import java.util.UUID;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobContainerClient blobContainerClient;
    private final Tika tika;

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
            return tika.detect(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to detect MIME type from blob", e);
        }
    }
}
