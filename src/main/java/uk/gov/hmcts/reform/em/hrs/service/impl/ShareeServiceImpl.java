package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.ShareeService;

@Component
@Transactional
public class ShareeServiceImpl implements ShareeService {
    private final ShareesRepository shareesRepository;

    @Autowired
    public ShareeServiceImpl(
        final ShareesRepository shareesRepository) {
        this.shareesRepository = shareesRepository;
    }

    public HearingRecordingSharee createAndSaveEntry(final String emailAddress,
                                                     final HearingRecording hearingRecording) {
        final HearingRecordingSharee hearingRecordingSharee = HearingRecordingSharee.builder()
            .hearingRecording(hearingRecording)
            .shareeEmail(emailAddress).build();
        return shareesRepository.save(hearingRecordingSharee);
    }

}
