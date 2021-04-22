package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import javax.inject.Inject;
import javax.inject.Named;

@Named
@Transactional
public class ShareeServiceImpl implements ShareeService {
    private final ShareesRepository shareesRepository;

    @Inject
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
