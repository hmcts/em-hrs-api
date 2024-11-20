package uk.gov.hmcts.reform.em.hrs.service.email;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;

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
        "Service Code", "File Size KB", "CCD Case Id", "Date Processed", "Shared On", "Sharee Email"
    };

    public File writeHearingRecordingSummaryToCsv(List<AuditEntry> data) throws IOException {
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
            for (AuditEntry entry : Optional.ofNullable(data).orElse(emptyList())) {
                if (entry instanceof HearingRecordingSegmentAuditEntry) {
                    HearingRecordingSegmentAuditEntry hearingAudit = (HearingRecordingSegmentAuditEntry) entry;
                    var hearingRecSeg = hearingAudit.getHearingRecordingSegment();
                    printer.printRecord(
                        entry.getAction(),
                        entry.getUsername(),
                        hearingRecSeg.getFilename(),
                        hearingRecSeg.getIngestionFileSourceUri(),
                        hearingRecSeg.getHearingRecording().getHearingSource(),
                        hearingRecSeg.getHearingRecording().getServiceCode(),
                        (int) Math.ceil((float) hearingRecSeg.getFileSizeMb() / 1000),
                        hearingRecSeg.getHearingRecording().getCcdCaseId(),
                        hearingRecSeg.getCreatedOn(),
                        "",
                        ""
                    );
                } else if (entry instanceof HearingRecordingAuditEntry) {
                    HearingRecordingAuditEntry hearingRecordingAudit = (HearingRecordingAuditEntry) entry;
                    var hearingRec = hearingRecordingAudit.getHearingRecording();
                    printer.printRecord(
                        entry.getAction(),
                        entry.getUsername(),
                        "",
                        "",
                        hearingRec.getHearingSource(),
                        hearingRec.getServiceCode(),
                        "",
                        hearingRec.getCcdCaseId(),
                        hearingRec.getCreatedOn(),
                        "",
                        ""
                    );
                } else if (entry instanceof HearingRecordingShareeAuditEntry) {
                    HearingRecordingShareeAuditEntry hearingShareeAudit = (HearingRecordingShareeAuditEntry) entry;
                    var hearingSharee = hearingShareeAudit.getHearingRecordingSharee();
                    var hearingRecording = hearingSharee.getHearingRecording();
                    printer.printRecord(
                        entry.getAction(),
                        entry.getUsername(),
                        "",
                        "",
                        hearingRecording.getHearingSource(),
                        hearingRecording.getServiceCode(),
                        "",
                        hearingRecording.getCcdCaseId(),
                        hearingRecording.getCreatedOn(),
                        hearingSharee.getSharedOn(),
                        hearingSharee.getShareeEmail()
                    );

                }
            }
        }
        return csvFile;
    }
}
