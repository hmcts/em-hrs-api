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
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.time.LocalDateTime;
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

    @Test
    void shouldReturnHearingRecordingSegmentAuditEntriesWithinDateRange() {
        // given
        var startDate = LocalDateTime.now().minusDays(30);
        var endDate = LocalDateTime.now();

        var auditEntry1 = new HearingRecordingSegmentAuditEntry();
        var auditEntry2 = new HearingRecordingSegmentAuditEntry();

        when(hearingRecordingAuditEntryRepository.findByEventDateTimeBetween(startDate, endDate))
            .thenReturn(List.of(auditEntry1, auditEntry2));

        // when
        List<HearingRecordingSegmentAuditEntry> result =
            auditEntryService.listHearingRecordingAudits(startDate, endDate);

        // then
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(auditEntry1));
        Assertions.assertTrue(result.contains(auditEntry2));
        verify(hearingRecordingAuditEntryRepository, times(1))
            .findByEventDateTimeBetween(startDate, endDate);
    }

    @Test
    void shouldReturnEmptyListWhenNoAuditEntriesFound() {
        // given
        var startDate = LocalDateTime.now().minusDays(30);
        var endDate = LocalDateTime.now();

        when(hearingRecordingAuditEntryRepository.findByEventDateTimeBetween(startDate, endDate))
            .thenReturn(List.of());

        // when
        List<HearingRecordingSegmentAuditEntry> result =
            auditEntryService.listHearingRecordingAudits(startDate, endDate);

        // then
        Assertions.assertTrue(result.isEmpty());
        verify(hearingRecordingAuditEntryRepository, times(1))
            .findByEventDateTimeBetween(startDate, endDate);
    }

    @Test
    void shouldThrowExceptionWhenRepositoryThrows() {
        // given
        var startDate = LocalDateTime.now().minusDays(30);
        var endDate = LocalDateTime.now();

        when(hearingRecordingAuditEntryRepository.findByEventDateTimeBetween(startDate, endDate))
            .thenThrow(new RuntimeException("Database error"));

        // when / then
        Assertions.assertThrows(
            RuntimeException.class,
            () -> auditEntryService.listHearingRecordingAudits(startDate, endDate)
        );
        verify(hearingRecordingAuditEntryRepository, times(1))
            .findByEventDateTimeBetween(startDate, endDate);
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
