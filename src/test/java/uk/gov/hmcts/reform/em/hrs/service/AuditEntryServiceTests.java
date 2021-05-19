package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuditEntryServiceTests {

    @InjectMocks
    AuditEntryService auditEntryService;
    @Mock
    private HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;
    @Mock
    private SecurityService securityService;
    @Mock
    private HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;

    @Test
    public void testCreateAndSaveEntryForHearingRecording() {

        when(securityService.getAuditUserEmail()).thenReturn("x");
        when(securityService.getCurrentlyAuthenticatedServiceName()).thenReturn("s");

        HearingRecordingAuditEntry entry = auditEntryService.createAndSaveEntry(
            new HearingRecording(),
            AuditActions.DATA_LIFECYCLE_CREATED
        );

        Assertions.assertEquals("x", entry.getUsername());
        Assertions.assertEquals("s", entry.getServiceName());

        verify(hearingRecordingAuditEntryRepository, times(1)).save(any(HearingRecordingAuditEntry.class));
    }

    @Test
    public void testFindHearingRecordingAudits() {
        when(hearingRecordingAuditEntryRepository
                 .findByHearingRecordingOrderByEventDateTimeAsc(TestUtil.HEARING_RECORDING))
            .thenReturn(Stream.of(new HearingRecordingAuditEntry()).collect(Collectors.toList()));
        List<HearingRecordingAuditEntry> entries =
            auditEntryService.findHearingRecordingAudits(TestUtil.HEARING_RECORDING);
        Assertions.assertEquals(1, entries.size());
    }

    @Test
    public void testCreateAndSaveEntryForHearingRecordingSegment() {

        when(securityService.getAuditUserEmail()).thenReturn("x");
        when(securityService.getCurrentlyAuthenticatedServiceName()).thenReturn("s");

        HearingRecordingSegmentAuditEntry entry = auditEntryService.createAndSaveEntry(
            new HearingRecordingSegment(),
            AuditActions.USER_DOWNLOAD_REQUESTED
        );

        Assertions.assertEquals("x", entry.getUsername());
        Assertions.assertEquals("s", entry.getServiceName());

        verify(hearingRecordingSegmentAuditEntryRepository, times(1)).save(any(HearingRecordingSegmentAuditEntry.class));
    }
}
