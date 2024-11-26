package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditReportCsvWriterTest {

    private final AuditReportCsvWriter auditReportCsvWriter = new AuditReportCsvWriter();
    private static final AuditActions ACTION = AuditActions.USER_DOWNLOAD_OK;

    @Test
    void shouldWriteCsvWithHearingRecordingSegmentAuditEntry() throws IOException {
        // Given
        HearingRecordingSegment segment = new HearingRecordingSegment();
        segment.setFilename("file1.wav");
        segment.setIngestionFileSourceUri("source-uri-1");
        segment.setFileSizeMb(1024L);

        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setHearingSource("HearingSource1");
        hearingRecording.setServiceCode("Service1");
        hearingRecording.setCcdCaseId(12345L);
        segment.setHearingRecording(hearingRecording);

        HearingRecordingSegmentAuditEntry segmentAuditEntry = new HearingRecordingSegmentAuditEntry();
        segmentAuditEntry.setAction(ACTION);
        segmentAuditEntry.setUsername("user1");
        segmentAuditEntry.setEventDateTime(new Date());
        segmentAuditEntry.setHearingRecordingSegment(segment);

        // When
        File resultFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(List.of(segmentAuditEntry));

        // Then
        assertCsvContent(resultFile, 1);
    }

    @Test
    void shouldWriteCsvWithHearingRecordingAuditEntry() throws IOException {
        // Given
        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setHearingSource("HearingSource2");
        hearingRecording.setServiceCode("Service2");
        hearingRecording.setCcdCaseId(23456L);
        hearingRecording.setCreatedOn(LocalDateTime.now());

        HearingRecordingAuditEntry recordingAuditEntry = new HearingRecordingAuditEntry();
        recordingAuditEntry.setAction(ACTION);
        recordingAuditEntry.setUsername("user2");
        recordingAuditEntry.setHearingRecording(hearingRecording);

        // When
        File resultFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(List.of(recordingAuditEntry));

        // Then
        assertCsvContent(resultFile, 1);
    }

    @Test
    void shouldWriteCsvWithHearingRecordingShareeAuditEntry() throws IOException {
        // Given
        HearingRecordingSharee sharee = new HearingRecordingSharee();
        sharee.setShareeEmail("sharee@example.com");
        sharee.setSharedOn(LocalDateTime.now());

        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setHearingSource("HearingSource3");
        hearingRecording.setServiceCode("Service3");
        hearingRecording.setCcdCaseId(34567L);
        sharee.setHearingRecording(hearingRecording);

        HearingRecordingShareeAuditEntry shareeAuditEntry = new HearingRecordingShareeAuditEntry();
        shareeAuditEntry.setAction(ACTION);
        shareeAuditEntry.setUsername("user3");
        shareeAuditEntry.setHearingRecordingSharee(sharee);

        // When
        File resultFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(List.of(shareeAuditEntry));

        // Then
        assertCsvContent(resultFile, 1);
    }

    @Test
    void shouldHandleEmptyListGracefully() throws IOException {
        // When
        File resultFile = auditReportCsvWriter.writeHearingRecordingSummaryToCsv(List.of());

        // Then
        assertCsvContent(resultFile, 0);
    }

    private void assertCsvContent(File resultFile, int expectedRows) throws IOException {
        assertNotNull(resultFile);
        assertEquals(".csv", resultFile.getName().substring(resultFile.getName().length() - 4));

        try (FileReader reader = new FileReader(resultFile);
             CSVParser parser = new CSVParser(
                 reader,
                 CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
             )) {

            List<CSVRecord> records = parser.getRecords();
            assertEquals(expectedRows, records.size());
        }
    }
}
