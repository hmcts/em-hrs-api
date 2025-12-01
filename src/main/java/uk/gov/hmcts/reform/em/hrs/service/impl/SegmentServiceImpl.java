package uk.gov.hmcts.reform.em.hrs.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.Mp4MimeTypeService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;

import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final HearingRecordingSegmentRepository segmentRepository;
    private final BlobContainerClient blobContainerClient;
    private final Mp4MimeTypeService mp4Inspector;
    private static final Logger LOGGER = getLogger(SegmentServiceImpl.class);


    @Autowired
    public SegmentServiceImpl(
        final HearingRecordingSegmentRepository segmentRepository,
        @Qualifier("hrsCvpBlobContainerClient") final BlobContainerClient blobContainerClient,
        final Mp4MimeTypeService mp4MimeTypeService
    ) {
        this.segmentRepository = segmentRepository;
        this.blobContainerClient = blobContainerClient;
        this.mp4Inspector = mp4MimeTypeService;
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

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String mimeType = mp4Inspector.getMimeType(
            blobContainerClient.getBlobClient(recordingDto.getFilename())
        );
        stopWatch.stop();

        LOGGER.info("Mime type detection took {} ms for {}",
                    stopWatch.getDuration().toMillis(), recordingDto.getFilename());

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
}
