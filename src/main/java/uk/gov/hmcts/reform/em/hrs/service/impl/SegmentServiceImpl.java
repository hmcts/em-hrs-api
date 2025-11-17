package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final HearingRecordingSegmentRepository segmentRepository;

    @Autowired
    public SegmentServiceImpl(final HearingRecordingSegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
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
        return HearingRecordingSegment.builder()
            .filename(recordingDto.getFilename())
            .fileExtension(recordingDto.getFilenameExtension())
            .fileSizeMb(recordingDto.getFileSize())
            .fileMd5Checksum(recordingDto.getCheckSum())
            .ingestionFileSourceUri(recordingDto.getSourceBlobUrl())
            .recordingSegment(recordingDto.getSegment())
            .hearingRecording(hearingRecording)
            .interpreter(recordingDto.getInterpreter())
            .build();
    }
}
