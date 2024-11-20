package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditReportCsvWriterTest {

    private static final AuditActions ACTION = AuditActions.USER_DOWNLOAD_OK;
    private static final String USERNAME = "test-user";
    private static final String FILE_NAME = "file.mp4";
    private static final String SOURCE_URI = "https://example.com/file.mp4";
    private static final LocalDateTime CREATED_ON = LocalDateTime.now();

    private final AuditReportCsvWriter auditReportCsvWriter = new AuditReportCsvWriter();

    @Test
    void should_write_audit_summary_to_csv() throws IOException {
        // Prepare mock data
        HearingRecordingSegmentAuditEntry mockAuditEntry = mock(HearingRecordingSegmentAuditEntry.class);
        HearingRecordingSegment mockSegment = mock(HearingRecordingSegment.class);
        HearingRecording mockRecording = mock(HearingRecording.class);

        // Mock HearingRecordingSegment
        when(mockSegment.getFilename()).thenReturn(FILE_NAME);
        when(mockSegment.getIngestionFileSourceUri()).thenReturn(SOURCE_URI);
        when(mockSegment.getFileSizeMb()).thenReturn(12023L);
        when(mockSegment.getCreatedOn()).thenReturn(CREATED_ON);
        when(mockSegment.getHearingRecording()).thenReturn(mockRecording);

        // Mock HearingRecording
        when(mockRecording.getHearingSource()).thenReturn("hearing-source VH");
        when(mockRecording.getServiceCode()).thenReturn("servicecode-1");
        when(mockRecording.getCcdCaseId()).thenReturn(1234567L);

        // Mock HearingRecordingSegmentAuditEntry
        when(mockAuditEntry.getAction()).thenReturn(ACTION);
        when(mockAuditEntry.getUsername()).thenReturn(USERNAME);
        when(mockAuditEntry.getHearingRecordingSegment()).thenReturn(mockSegment);

        List<HearingRecordingSegmentAuditEntry> data = Collections.singletonList(mockAuditEntry);

        // Execute method
        File csvFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        // Assertions
        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(2, lines.size()); // Header + 1 row of data

        assertEquals(
            "Action,UserName,File Name,Source URI,Hearing Source,Service Code,File Size KB,CCD Case Id,Date Processed",
            lines.get(0)
        );

        String expectedRow = String.format(
            "%s,%s,%s,%s,%s,%s,%d,%d,%s",
            ACTION,
            USERNAME,
            FILE_NAME,
            SOURCE_URI,
            "hearing-source VH",
            "servicecode-1",
            13, // File size in KB (rounded up)
            1234567,
            CREATED_ON
        );
        assertEquals(expectedRow, lines.get(1));
    }

    @Test
    void should_write_empty_audit_summary_to_csv() throws IOException {
        // Empty list input
        List<HearingRecordingSegmentAuditEntry> data = Collections.emptyList();

        // Execute method
        File csvFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        // Assertions
        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(1, lines.size()); // Only the header

        assertEquals(
            "Action,UserName,File Name,Source URI,Hearing Source,Service Code,File Size KB,CCD Case Id,Date Processed",
            lines.get(0)
        );
    }
}
