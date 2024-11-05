package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.auditlog.AuditLogFormatter;
import uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEntryServiceTests {

    private static final String USER_EMAIL = "email@hmcts.net.internal";
    private static final String SERVICE_NAME = "SUT";
    private static final String CLIENT_IP = "127.0.0.1";

    @InjectMocks
    private AuditEntryService auditEntryService;

    @Mock
    private SecurityService securityService;

    @Mock
    private HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;

    @Mock
    private HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;

    @Mock
    private ShareesAuditEntryRepository shareesAuditEntryRepository;

    private HearingRecording hearingRecording;
    private HearingRecordingSegment hearingRecordingSegment;
    private HearingRecordingSharee hearingRecordingSharee;

    @Mock
    private AuditLogFormatter auditLogFormatter;

    @BeforeEach
    void prepare() {
        hearingRecording = new HearingRecording();
        hearingRecording.setCcdCaseId(1234L);
        hearingRecordingSegment = new HearingRecordingSegment();
        hearingRecordingSegment.setHearingRecording(hearingRecording);
        hearingRecordingSharee = new HearingRecordingSharee();
        hearingRecordingSharee.setHearingRecording(hearingRecording);

    }

    @Test
    void testFindHearingRecordingAudits() {
        HearingRecording testHearingRecording = TestUtil.hearingRecordingWithNoDataBuilder();

        when(hearingRecordingAuditEntryRepository
                 .findByHearingRecordingOrderByEventDateTimeAsc(testHearingRecording))
            .thenReturn(Stream.of(new HearingRecordingAuditEntry()).toList());
        List<HearingRecordingAuditEntry> entries =
            auditEntryService.findHearingRecordingAudits(testHearingRecording);
        Assertions.assertEquals(1, entries.size());
    }

    @Test
    void testCreateAndSaveEntryForHearingRecording() {
        prepareMockSecurityService();

        HearingRecordingAuditEntry entry = auditEntryService.createAndSaveEntry(
            hearingRecording,
            AuditActions.USER_DOWNLOAD_REQUESTED
        );

        assertSecurityServiceValues(entry);
        assertLogFormatterInvoked();
        verify(hearingRecordingAuditEntryRepository, times(1))
            .save(entry);
    }

    @Test
    void testCreateAndSaveEntryForHearingRecordingSegment() {
        prepareMockSecurityService();

        HearingRecordingSegmentAuditEntry entry = auditEntryService.createAndSaveEntry(
            hearingRecordingSegment,
            AuditActions.USER_DOWNLOAD_REQUESTED
        );

        assertSecurityServiceValues(entry);
        assertLogFormatterInvoked();
        verify(hearingRecordingSegmentAuditEntryRepository, times(1))
            .save(entry);
    }

    @Test
    void testCreateAndSaveEntryForHearingRecordingSharee() {
        prepareMockSecurityService();

        HearingRecordingShareeAuditEntry entry = auditEntryService.createAndSaveEntry(
            hearingRecordingSharee,
            AuditActions.SHARE_GRANT_OK
        );

        assertSecurityServiceValues(entry);
        assertLogFormatterInvoked();

        verify(shareesAuditEntryRepository, times(1))
            .save(entry);

    }

    @Test
    void testLogsForNonEntity() {
        prepareMockSecurityService();

        auditEntryService.logOnly(
            1234L,
            AuditActions.NOTIFY_OK
        );

        assertLogFormatterInvoked();
    }

    private void prepareMockSecurityService() {
        when(securityService.getAuditUserEmail()).thenReturn(USER_EMAIL);
        when(securityService.getCurrentlyAuthenticatedServiceName()).thenReturn(SERVICE_NAME);
        when(securityService.getClientIp()).thenReturn(CLIENT_IP);
    }

    private void assertSecurityServiceValues(AuditEntry entry) {
        Assertions.assertEquals(USER_EMAIL, entry.getUsername());
        Assertions.assertEquals(SERVICE_NAME, entry.getServiceName());
        Assertions.assertEquals(CLIENT_IP, entry.getIpAddress());
    }

    private void assertLogFormatterInvoked() {
        verify(auditLogFormatter, times(1)).format(any(AuditEntry.class));
    }
}
