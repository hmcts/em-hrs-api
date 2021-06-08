package uk.gov.hmcts.reform.em.hrs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.auditlog.AuditLogFormatter;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;
import uk.gov.hmcts.reform.em.hrs.model.LogOnlyAuditEntry;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;

import java.util.Date;
import java.util.List;

@Service
public class AuditEntryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEntryService.class);

    @Autowired
    private HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;

    @Autowired
    private HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;

    @Autowired
    private ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AuditLogFormatter auditLogFormatter;

    void logOnly(String caseId, AuditActions action) {
        var entry = new LogOnlyAuditEntry();
        populateCommonFields(
            entry,
            action,
            caseId
        );
        LOGGER.info(auditLogFormatter.format(entry));

    }

    List<HearingRecordingAuditEntry> findHearingRecordingAudits(HearingRecording hearingRecording) {
        return hearingRecordingAuditEntryRepository.findByHearingRecordingOrderByEventDateTimeAsc(hearingRecording);
    }


    public HearingRecordingAuditEntry createAndSaveEntry(HearingRecording hearingRecording,
                                                         AuditActions action) {
        var entry = new HearingRecordingAuditEntry();
        entry.setHearingRecording(hearingRecording);

        String caseId = hearingRecording.getCcdCaseId().toString();
        populateCommonFields(
            entry,
            action,
            caseId
        );

        LOGGER.info(auditLogFormatter.format(entry));
        hearingRecordingAuditEntryRepository.save(entry);
        return entry;

    }

    public HearingRecordingSegmentAuditEntry createAndSaveEntry(HearingRecordingSegment hearingRecordingSegment,
                                                                AuditActions action) {

        var entry = new HearingRecordingSegmentAuditEntry(hearingRecordingSegment);

        String caseId = hearingRecordingSegment.getHearingRecording().getCcdCaseId().toString();
        populateCommonFields(
            entry,
            action,
            caseId
        );

        LOGGER.info(auditLogFormatter.format(entry));
        hearingRecordingSegmentAuditEntryRepository.save(entry);
        return entry;
    }


    public HearingRecordingShareeAuditEntry createAndSaveEntry(HearingRecordingSharee hearingRecordingSharee,
                                                               AuditActions action) {

        var entry = new HearingRecordingShareeAuditEntry(hearingRecordingSharee);
        String caseId = hearingRecordingSharee.getHearingRecording().getCcdCaseId().toString();

        populateCommonFields(
            entry,
            action,
            caseId
        );

        LOGGER.info(auditLogFormatter.format(entry));
        hearingRecordingShareeAuditEntryRepository.save(entry);
        return entry;
    }

    //helpers
    private void populateCommonFields(AuditEntry auditEntry,
                                      AuditActions action,
                                      String caseId) {


        auditEntry.setAction(action);
        auditEntry.setUsername(securityService.getAuditUserEmail());
        auditEntry.setServiceName(securityService.getCurrentlyAuthenticatedServiceName());
        auditEntry.setIpAddress(securityService.getClientIp());
        auditEntry.setCaseId(caseId);
        auditEntry.setEventDateTime(new Date());

    }


}
