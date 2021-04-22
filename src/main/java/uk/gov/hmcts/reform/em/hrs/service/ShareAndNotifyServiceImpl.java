package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.exception.GovNotifyErrorException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CaseDataContentCreator;
import uk.gov.hmcts.reform.em.hrs.util.EmailValidator;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShareAndNotifyServiceImpl implements ShareAndNotifyService {
    private final HearingRecordingRepository hearingRecordingRepository;
    private final ShareeService shareeService;
    private final NotificationService notificationService;
    private final CaseDataContentCreator caseDataCreator;

    @Autowired
    public ShareAndNotifyServiceImpl(final HearingRecordingRepository hearingRecordingRepository,
                                     final ShareeService shareeService,
                                     final NotificationService notificationService,
                                     final CaseDataContentCreator caseDataCreator) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.shareeService = shareeService;
        this.notificationService = notificationService;
        this.caseDataCreator = caseDataCreator;
    }

    @Override
    public void shareAndNotify(final Long caseId, Map<String, Object> caseData, final String authorisationToken) {

        String shareeEmailAddress = caseData.get("recipientEmailAddress").toString();

        if (!EmailValidator.isValid(shareeEmailAddress)) {
            throw new ValidationErrorException(Map.of("recipientEmailAddress", shareeEmailAddress));
        }

        List<String> segmentUrls = caseDataCreator.extractRecordingFiles(caseData).stream()
            .map(recordingFile -> recordingFile.getCaseDocument())
            .map(caseDocument ->  caseDocument.getBinaryUrl())
            .collect(Collectors.toList());

        final HearingRecording hearingRecording = hearingRecordingRepository.findByCcdCaseId(caseId)
            .orElseThrow(() -> new HearingRecordingNotFoundException(caseId));

        final HearingRecordingSharee sharee = shareeService.createAndSaveEntry(
            shareeEmailAddress,
            hearingRecording
        );

        try {
            notificationService.sendEmailNotification(
                hearingRecording.getCaseRef(),
                hearingRecording.getCreatedOn(),
                List.copyOf(segmentUrls),
                sharee.getId(),
                shareeEmailAddress
            );
        } catch (NotificationClientException e) {
            throw new GovNotifyErrorException(e);
        }
    }
}
