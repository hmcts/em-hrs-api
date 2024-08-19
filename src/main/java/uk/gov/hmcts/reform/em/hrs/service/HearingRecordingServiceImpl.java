package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class HearingRecordingServiceImpl implements HearingRecordingService {

    private final HearingRecordingRepository hearingRecordingRepository;
    private final BlobStorageDeleteService blobStorageDeleteService;

    public HearingRecordingServiceImpl(HearingRecordingRepository hearingRecordingRepository,
                                       BlobStorageDeleteService blobStorageDeleteService) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.blobStorageDeleteService = blobStorageDeleteService;
    }

    @Override
    public void deleteCaseHearingRecordings(Collection<Long> ccdCaseIds) {
        List<HearingRecording> hearingRecordings = hearingRecordingRepository.deleteByCcdCaseIdIn(ccdCaseIds);
        List<HearingRecordingSegment> segments =
            hearingRecordings.stream().flatMap(hearingRecording -> hearingRecording.getSegments().stream()).toList();

        segments.forEach(segment -> blobStorageDeleteService.deleteBlob(
            segment.getFilename(), HearingSource.valueOf(segment.getHearingRecording().getHearingSource())));
    }
}
