package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock
    private AuditEntryService auditEntryService;

    @Mock
    private AuditReportCsvWriter auditReportCsvWriter;

    private AuditReportService auditReportService;

    @BeforeEach
    void setUp() {
        auditReportService = new AuditReportService(auditEntryService, auditReportCsvWriter);
    }

    @Test
    void createMonthlyReport_shouldGenerateReportSuccessfully() throws IOException {
        // Arrange
        Month month = Month.JANUARY;
        int year = 2025;
        LocalDateTime startOfMonth = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endOfMonth = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

        List<AuditEntry> mockAuditEntries = new ArrayList<>();
        when(auditEntryService.listHearingRecordingAudits(startOfMonth, endOfMonth)).thenReturn(mockAuditEntries);

        File mockFile = new File("mockReport.csv");
        when(auditReportCsvWriter.writeHearingRecordingSummaryToCsv(mockAuditEntries)).thenReturn(mockFile);

        // Act
        File result = auditReportService.createMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertEquals(mockFile, result);
        verify(auditEntryService).listHearingRecordingAudits(startOfMonth, endOfMonth);
        verify(auditReportCsvWriter).writeHearingRecordingSummaryToCsv(mockAuditEntries);
    }

    @Test
    void createMonthlyReport_shouldHandleLeapYear() throws IOException {
        // Arrange
        Month month = Month.FEBRUARY;
        int year = 2024;
        LocalDateTime startOfMonth = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime endOfMonth = LocalDateTime.of(2024, 2, 29, 23, 59, 59);

        List<AuditEntry> mockAuditEntries = new ArrayList<>();
        when(auditEntryService.listHearingRecordingAudits(startOfMonth, endOfMonth)).thenReturn(mockAuditEntries);
        when(auditReportCsvWriter.writeHearingRecordingSummaryToCsv(mockAuditEntries))
            .thenReturn(new File("mockReport.csv"));

        // Act
        auditReportService.createMonthlyReport(month, year);

        // Assert
        verify(auditEntryService).listHearingRecordingAudits(startOfMonth, endOfMonth);
        verify(auditReportCsvWriter).writeHearingRecordingSummaryToCsv(mockAuditEntries);
    }

    @Test
    void createMonthlyReport_shouldThrowIOException() throws IOException {
        // Arrange
        Month month = Month.MARCH;
        int year = 2025;
        LocalDateTime startOfMonth = LocalDateTime.of(2025, 3, 1, 0, 0);
        LocalDateTime endOfMonth = LocalDateTime.of(2025, 3, 31, 23, 59, 59);

        List<AuditEntry> mockAuditEntries = new ArrayList<>();
        when(auditEntryService.listHearingRecordingAudits(startOfMonth, endOfMonth)).thenReturn(mockAuditEntries);
        when(auditReportCsvWriter.writeHearingRecordingSummaryToCsv(mockAuditEntries)).thenThrow(new IOException(
            "Test exception"));

        // Act & Assert
        assertThrows(IOException.class, () -> auditReportService.createMonthlyReport(month, year));
    }
}
