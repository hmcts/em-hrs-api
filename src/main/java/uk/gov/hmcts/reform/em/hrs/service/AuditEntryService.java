package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;

import java.util.Date;
import java.util.List;

@Service
public class AuditEntryService {

    @Autowired
    private HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;

    @Autowired
    private HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;

    @Autowired
    private SecurityService securityService;

    public List<HearingRecordingAuditEntry> findHearingRecordingAudits(HearingRecording hearingRecording) {
        return hearingRecordingAuditEntryRepository.findByHearingRecordingOrderByEventDateTimeAsc(hearingRecording);
    }

    public HearingRecordingAuditEntry createAndSaveEntry(HearingRecording hearingRecording,
                                                         AuditActions action) {
        return createAndSaveEntry(
            hearingRecording,
            action,
            securityService.getAuditUserEmail(),
            securityService.getCurrentlyAuthenticatedServiceName());
    }

    public HearingRecordingAuditEntry createAndSaveEntry(HearingRecording hearingRecording,
                                                         AuditActions action,
                                                         String username,
                                                         String serviceName) {
        HearingRecordingAuditEntry hearingRecordingAuditEntry = new HearingRecordingAuditEntry();
        populateCommonFields(hearingRecordingAuditEntry, action, username, serviceName);
        hearingRecordingAuditEntry.setHearingRecording(hearingRecording);
        hearingRecordingAuditEntryRepository.save(hearingRecordingAuditEntry);
        return hearingRecordingAuditEntry;
    }

    public HearingRecordingSegmentAuditEntry createAndSaveEntry(HearingRecordingSegment hearingRecordingSegment,
                                                         AuditActions action) {
        return createAndSaveEntry(
            hearingRecordingSegment,
            action,
            securityService.getAuditUserEmail(),
            securityService.getCurrentlyAuthenticatedServiceName());
    }

    public HearingRecordingSegmentAuditEntry createAndSaveEntry(HearingRecordingSegment hearingRecordingSegment,
                                                         AuditActions action,
                                                         String username,
                                                         String serviceName) {
        HearingRecordingSegmentAuditEntry hearingRecordingSegmentAuditEntry = new HearingRecordingSegmentAuditEntry();
        populateCommonFields(hearingRecordingSegmentAuditEntry, action, username, serviceName);
        hearingRecordingSegmentAuditEntry.setHearingRecordingSegment(hearingRecordingSegment);
        hearingRecordingSegmentAuditEntryRepository.save(hearingRecordingSegmentAuditEntry);
        return hearingRecordingSegmentAuditEntry;
    }

    private void populateCommonFields(AuditEntry auditEntry,
                                      AuditActions action,
                                      String username,
                                      String serviceName) {
        auditEntry.setAction(action);
        auditEntry.setUsername(username);
        auditEntry.setServiceName(serviceName);
        auditEntry.setEventDateTime(new Date());
    }


}
