package uk.gov.hmcts.reform.em.hrs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.reform.em.hrs.repository.AuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class AuditEntryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEntryService.class);

    private final HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;

    private final HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;

    private final ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository;

    private final SecurityService securityService;

    private final AuditLogFormatter auditLogFormatter;

    private final AuditEntryRepository auditEntryRepository;


    @Autowired
    public AuditEntryService(
        HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository,
        HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository,
        ShareesAuditEntryRepository hearingRecordingShareeAuditEntryRepository,
        SecurityService securityService, AuditLogFormatter auditLogFormatter,
        AuditEntryRepository auditEntryRepository
    ) {
        this.hearingRecordingAuditEntryRepository = hearingRecordingAuditEntryRepository;
        this.hearingRecordingSegmentAuditEntryRepository = hearingRecordingSegmentAuditEntryRepository;
        this.hearingRecordingShareeAuditEntryRepository = hearingRecordingShareeAuditEntryRepository;
        this.securityService = securityService;
        this.auditLogFormatter = auditLogFormatter;
        this.auditEntryRepository = auditEntryRepository;
    }

    public void logOnly(Long caseId, AuditActions action) {
        var entry = new LogOnlyAuditEntry();
        populateCommonFields(
            entry,
            action,
            caseId
        );
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(auditLogFormatter.format(entry));
        }

    }

    public List<HearingRecordingAuditEntry> findHearingRecordingAudits(HearingRecording hearingRecording) {
        return hearingRecordingAuditEntryRepository.findByHearingRecordingOrderByEventDateTimeAsc(hearingRecording);
    }

    @Transactional
    public List<AuditEntry> listHearingRecordingAudits(
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return auditEntryRepository.findByEventDateTimeBetween(startDate, endDate);
    }

    public HearingRecordingAuditEntry createAndSaveEntry(HearingRecording hearingRecording,
                                                         AuditActions action) {
        var entry = new HearingRecordingAuditEntry(hearingRecording);

        Long caseId = hearingRecording.getCcdCaseId();
        populateCommonFields(
            entry,
            action,
            caseId
        );

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(auditLogFormatter.format(entry));
        }
        hearingRecordingAuditEntryRepository.save(entry);
        return entry;

    }

    public HearingRecordingSegmentAuditEntry createAndSaveEntry(HearingRecordingSegment hearingRecordingSegment,
                                                                AuditActions action) {

        var entry = new HearingRecordingSegmentAuditEntry(hearingRecordingSegment);

        Long caseId = hearingRecordingSegment.getHearingRecording().getCcdCaseId();
        populateCommonFields(
            entry,
            action,
            caseId
        );

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(auditLogFormatter.format(entry));
        }
        hearingRecordingSegmentAuditEntryRepository.save(entry);
        return entry;
    }


    public HearingRecordingShareeAuditEntry createAndSaveEntry(HearingRecordingSharee hearingRecordingSharee,
                                                               AuditActions action) {

        var entry = new HearingRecordingShareeAuditEntry(hearingRecordingSharee);
        Long caseId = hearingRecordingSharee.getHearingRecording().getCcdCaseId();

        populateCommonFields(
            entry,
            action,
            caseId
        );

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(auditLogFormatter.format(entry));
        }
        hearingRecordingShareeAuditEntryRepository.save(entry);
        return entry;
    }

    //helpers
    private void populateCommonFields(AuditEntry auditEntry,
                                      AuditActions action,
                                      Long caseId) {


        auditEntry.setAction(action);
        auditEntry.setUsername(securityService.getAuditUserEmail());
        auditEntry.setServiceName(securityService.getCurrentlyAuthenticatedServiceName());
        auditEntry.setIpAddress(securityService.getClientIp());
        auditEntry.setCaseId(caseId);
        auditEntry.setEventDateTime(new Date());

    }
}
