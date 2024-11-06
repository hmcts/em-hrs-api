package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.exception.GovNotifyErrorException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.Constants;
import uk.gov.hmcts.reform.em.hrs.service.NotificationService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;
import uk.gov.hmcts.reform.em.hrs.service.ShareeService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CaseDataContentCreator;
import uk.gov.hmcts.reform.em.hrs.util.EmailValidator;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;
import java.util.Map;

@Service
public class ShareAndNotifyServiceImpl implements ShareAndNotifyService {
    private static final Logger LOGGER =  LoggerFactory.getLogger(ShareAndNotifyServiceImpl.class);
    private final HearingRecordingRepository hearingRecordingRepository;
    private final ShareeService shareeService;
    private final NotificationService notificationService;
    private final CaseDataContentCreator caseDataCreator;
    private final String xuiDomain;
    private final AuditEntryService auditEntryService;

    @Autowired
    public ShareAndNotifyServiceImpl(final HearingRecordingRepository hearingRecordingRepository,
                                     final ShareeService shareeService,
                                     final NotificationService notificationService,
                                     final CaseDataContentCreator caseDataCreator,
                                     @Value("${xui.api.url}") final String xuiDomain,
                                     AuditEntryService auditEntryService) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.shareeService = shareeService;
        this.notificationService = notificationService;
        this.caseDataCreator = caseDataCreator;
        this.xuiDomain = xuiDomain;
        this.auditEntryService = auditEntryService;
    }

    @Override
    public void shareAndNotify(final Long caseId, Map<String, Object> caseDataMap, final String authorisationToken) {

        CaseHearingRecording caseData = caseDataCreator.getCaseRecordingObject(caseDataMap);

        if (!EmailValidator.isValid(caseData.getShareeEmail())) {
            auditEntryService.logOnly(caseId, AuditActions.SHARE_GRANT_FAIL);
            throw new ValidationErrorException(Map.of("recipientEmailAddress", caseData.getShareeEmail()));
        }

        final HearingRecording recording = hearingRecordingRepository.findByCcdCaseId(caseId)
            .orElseThrow(() -> new HearingRecordingNotFoundException(caseId));

        final HearingRecordingSharee sharee = shareeService.createAndSaveEntry(caseData.getShareeEmail(), recording);
        auditEntryService.createAndSaveEntry(sharee, AuditActions.SHARE_GRANT_OK);

        List<String> segmentUrls = caseDataCreator.extractCaseDocuments(caseData).stream()
            .map(CaseDocument::getBinaryUrl)
            .map(url -> {
                String downloadPath = url.substring(url.indexOf("/hearing-recordings"));
                return xuiDomain + downloadPath + Constants.SHAREE;
            })
            .toList();

        LOGGER.info("segmentUrls {}", segmentUrls);
        try {
            notificationService.sendEmailNotification(recording.getCaseRef(), List.copyOf(segmentUrls),
                                                      caseData.getRecordingDate(), caseData.getRecordingTimeOfDay(),
                                                      sharee.getId(), caseData.getShareeEmail()
            );
            auditEntryService.logOnly(caseId, AuditActions.NOTIFY_OK);
        } catch (NotificationClientException e) {
            auditEntryService.logOnly(caseId, AuditActions.NOTIFY_FAIL);
            throw new GovNotifyErrorException(e);
        }
    }
}
