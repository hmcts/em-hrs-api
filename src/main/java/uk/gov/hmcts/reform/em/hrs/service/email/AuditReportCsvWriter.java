package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Component
public class AuditReportCsvWriter {

    private static final String[] HEARING_AUDIT_SUMMARY_CSV_HEADERS = {
        "Action", "UserName", "File Name", "Source URI", "Hearing Source",
        "Service Code", "File Size KB", "CCD Case Id", "Date Processed"
    };

    public File writeHearingRecordingSummaryToCsv(
        List<HearingRecordingSegmentAuditEntry> data
    ) throws IOException {
        File csvFile = File.createTempFile(
            "hearing-audit-report",
            ".csv"
        );

        CSVFormat csvFileHeader = CSVFormat
            .Builder
            .create()
            .setHeader(HEARING_AUDIT_SUMMARY_CSV_HEADERS)
            .build();
        try (
            FileWriter fileWriter = new FileWriter(csvFile);
            CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)
        ) {
            for (HearingRecordingSegmentAuditEntry hearingAudit: Optional.ofNullable(data).orElse(emptyList())) {
                var hearingRecSeg = hearingAudit.getHearingRecordingSegment();
                printer.printRecord(
                    hearingAudit.getAction(),
                    hearingAudit.getUsername(),
                    hearingRecSeg.getFilename(),
                    hearingRecSeg.getIngestionFileSourceUri(),
                    hearingRecSeg.getHearingRecording().getHearingSource(),
                    hearingRecSeg.getHearingRecording().getServiceCode(),
                    (int)Math.ceil((float) hearingRecSeg.getFileSizeMb() / 1000),
                    hearingRecSeg.getHearingRecording().getCcdCaseId(),
                    hearingRecSeg.getCreatedOn()
                );
            }
        }
        return csvFile;
    }
}
