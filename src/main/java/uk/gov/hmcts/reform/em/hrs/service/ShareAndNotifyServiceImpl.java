package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.exception.GovNotifyErrorException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
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
    private final String xuiDomain;

    @Autowired
    public ShareAndNotifyServiceImpl(final HearingRecordingRepository hearingRecordingRepository,
                                     final ShareeService shareeService,
                                     final NotificationService notificationService,
                                     final CaseDataContentCreator caseDataCreator,
                                     @Value("${xui.api.url}") final String xuiDomain) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.shareeService = shareeService;
        this.notificationService = notificationService;
        this.caseDataCreator = caseDataCreator;
        this.xuiDomain = xuiDomain;
    }

    @Override
    public void shareAndNotify(final Long caseId, Map<String, Object> caseDataMap, final String authorisationToken) {

        CaseHearingRecording caseData = caseDataCreator.getCaseRecordingObject(caseDataMap);

        if (!EmailValidator.isValid(caseData.getShareeEmail())) {
            throw new ValidationErrorException(Map.of("recipientEmailAddress", caseData.getShareeEmail()));
        }

        List<String> segmentUrls = caseDataCreator.extractCaseDocuments(caseData).stream()
            .map(caseDocument ->  caseDocument.getBinaryUrl())
            .map(url -> {
                String downloadPath = url.substring(url.indexOf("/hearing-recordings"));
                return xuiDomain + downloadPath;
            })
            .collect(Collectors.toList());

        final HearingRecording recording = hearingRecordingRepository.findByCcdCaseId(caseId)
            .orElseThrow(() -> new HearingRecordingNotFoundException(caseId));

        final HearingRecordingSharee sharee = shareeService.createAndSaveEntry(caseData.getShareeEmail(), recording);

        try {
            notificationService.sendEmailNotification(recording.getCaseRef(), List.copyOf(segmentUrls),
                                                      caseData.getRecordingDate(), caseData.getRecordingTimeOfDay(),
                                                      sharee.getId(), caseData.getShareeEmail());
        } catch (NotificationClientException e) {
            throw new GovNotifyErrorException(e);
        }
    }
}
