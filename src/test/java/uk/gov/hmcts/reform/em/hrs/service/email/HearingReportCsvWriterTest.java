package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HearingReportCsvWriterTest {

    private static final String FILE_NAME = "file.mp4";
    private static final String SOURCE_URI = "https://example.com/file.mp4";
    private static final LocalDateTime CREATED_ON = LocalDateTime.now();


    @Test
    void should_write_hearingRecording_summary_to_csv() throws IOException {
        HearingReportCsvWriter hearingReportCsvWriter = new HearingReportCsvWriter();

        HearingRecordingSegment hearingRecordingSegment = new HearingRecordingSegment();
        hearingRecordingSegment.setFilename(FILE_NAME);
        hearingRecordingSegment.setIngestionFileSourceUri(SOURCE_URI);
        hearingRecordingSegment.setCreatedOn(CREATED_ON);

        List<HearingRecordingSegment> data = Arrays.asList(hearingRecordingSegment);

        File csvFile = hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(2, lines.size());
        assertEquals("File Name,Source URI,Date Processed", lines.get(0));
        assertEquals(String.format("%s,%s,%s", FILE_NAME, SOURCE_URI, CREATED_ON), lines.get(1));
    }

    @Test
    void should_write_empty_summary_to_csv() throws IOException {
        HearingReportCsvWriter hearingReportCsvWriter = new HearingReportCsvWriter();
        List<HearingRecordingSegment> data = Arrays.asList();

        File csvFile = hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(1, lines.size());
        assertEquals("File Name,Source URI,Date Processed", lines.get(0));
    }
}
