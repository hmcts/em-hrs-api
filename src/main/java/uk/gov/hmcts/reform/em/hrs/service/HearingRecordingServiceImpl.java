package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import java.util.Collection;

@Service
public class HearingRecordingServiceImpl implements HearingRecordingService {
    private final HearingRecordingRepository hearingRecordingRepository;

    public HearingRecordingServiceImpl(HearingRecordingRepository hearingRecordingRepository) {
        this.hearingRecordingRepository = hearingRecordingRepository;
    }

    @Override
    public long deleteCaseHearingRecordings(Collection<Long> ccdCaseIds) {
        return hearingRecordingRepository.deleteByCcdCaseIdIn(ccdCaseIds);
    }
}
