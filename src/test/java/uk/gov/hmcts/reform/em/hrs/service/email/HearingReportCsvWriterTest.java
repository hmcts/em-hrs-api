package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HearingReportCsvWriterTest {

    private static final String FILE_NAME = "file.mp4";
    private static final String SOURCE_URI = "https://example.com/file.mp4";
    private static final LocalDateTime CREATED_ON = LocalDateTime.now();

    private static final String HEADERS =
        "File Name,Source URI,Hearing Source,Service Code,File Size KB,CCD Case Id,Date Processed,Has Ttl Config";

    private TtlService ttlService = mock(TtlService.class);

    @Test
    void should_write_hearingRecording_summary_to_csv() throws IOException {
        HearingRecordingSegment hearingRecordingSegment = new HearingRecordingSegment();
        hearingRecordingSegment.setFilename(FILE_NAME);
        hearingRecordingSegment.setIngestionFileSourceUri(SOURCE_URI);
        hearingRecordingSegment.setCreatedOn(CREATED_ON);
        hearingRecordingSegment.setFileSizeMb(12023L);
        var hearingRecording = new HearingRecording();
        hearingRecording.setServiceCode("servicecode-1");
        hearingRecording.setHearingSource("hearing-source XX");
        hearingRecording.setCcdCaseId(1234567L);
        hearingRecordingSegment.setHearingRecording(hearingRecording);

        List<HearingRecordingSegment> data = Arrays.asList(hearingRecordingSegment);
        HearingReportCsvWriter hearingReportCsvWriter = new HearingReportCsvWriter(ttlService);

        when(ttlService.hasTtlConfig("servicecode-1", null))
            .thenReturn("Yes");

        File csvFile = hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(HEADERS, lines.get(0));
        assertEquals(String.format(
            "%s,%s,%s,%s,%s,%s,%s,%s",
            FILE_NAME,
            SOURCE_URI,
            "hearing-source XX",
            "servicecode-1",
            13,
            1234567,
            CREATED_ON,
            "Yes"
        ), lines.get(1));
    }

    @Test
    void should_write_empty_summary_to_csv() throws IOException {
        HearingReportCsvWriter hearingReportCsvWriter = new HearingReportCsvWriter(ttlService);
        List<HearingRecordingSegment> data = Arrays.asList();

        File csvFile = hearingReportCsvWriter.writeHearingRecordingSummaryToCsv(data);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.isFile());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(1, lines.size());
        assertEquals(HEADERS, lines.get(0));
    }
}
