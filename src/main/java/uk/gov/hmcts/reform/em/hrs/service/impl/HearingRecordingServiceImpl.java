package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.BlobStorageDeleteService;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class HearingRecordingServiceImpl implements HearingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(HearingRecordingServiceImpl.class);
    private final HearingRecordingRepository hearingRecordingRepository;
    private final BlobStorageDeleteService blobStorageDeleteService;

    public HearingRecordingServiceImpl(HearingRecordingRepository hearingRecordingRepository,
                                       BlobStorageDeleteService blobStorageDeleteService) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.blobStorageDeleteService = blobStorageDeleteService;
    }

    @Override
    public void deleteCaseHearingRecordings(Collection<Long> ccdCaseIds) {
        List<HearingRecording> hearingRecordings;
        try {
            hearingRecordings = hearingRecordingRepository.deleteByCcdCaseIdIn(ccdCaseIds);
        } catch (Exception e) {
            log.info("Database deletion failed for CCD Case IDs: {} with error: {}", ccdCaseIds, e.getMessage());
            return;
        }
        List<HearingRecordingSegment> segments =
            hearingRecordings.stream().flatMap(hearingRecording -> hearingRecording.getSegments().stream()).toList();

        segments.forEach(segment -> blobStorageDeleteService.deleteBlob(
            segment.getFilename(), HearingSource.valueOf(segment.getHearingRecording().getHearingSource())));
    }

    public Long findCcdCaseIdByFilename(String filename) {
        return hearingRecordingRepository.findCcdCaseIdByFilename(filename);
    }
}
