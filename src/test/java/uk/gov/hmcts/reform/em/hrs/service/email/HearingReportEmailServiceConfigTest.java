package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;


class HearingReportEmailServiceConfigTest {

    @Test
    void monthlyReportAttachmentName_createsCorrectFormat() {
        // Arrange
        LocalDate testDate = LocalDate.of(2023, Month.MARCH, 15);
        String prefix = "Monthly_Report_";

        // Act
        String result = HearingReportEmailServiceConfig.monthlyReportAttachmentName(prefix).apply(testDate);

        // Assert
        assertEquals("Monthly_Report_MARCH-2023.csv", result);
    }

    @Test
    void weeklyReportAttachmentName_createsCorrectFormat() {
        // Arrange
        LocalDate testDate = LocalDate.of(2023, Month.MARCH, 15);
        String prefix = "Weekly_Report_";

        // Act
        String result = HearingReportEmailServiceConfig.weeklyReportAttachmentName(prefix).apply(testDate);

        // Assert
        assertEquals("Weekly_Report_2023-03-08.csv", result);
    }

    @Test
    void weeklyReportEmailBody_containsCorrectDates() {
        // Arrange
        LocalDate testDate = LocalDate.of(2023, Month.MARCH, 15);

        // Act
        String result = HearingReportEmailServiceConfig.weeklyReportEmailBody(testDate);

        // Assert
        assertEquals(
            result,
            """
                <html>
                    <body>
                        <h1>Weekly Hearing Recording Report for week 2023-03-08</h1>
                        <br>
                        <br><br>
                    </body>
                </html>
                """
        );
    }


    @Test
    void attachmentNameMethods_handleEmptyPrefix() {
        // Arrange
        LocalDate testDate = LocalDate.of(2023, Month.APRIL, 1);

        // Act & Assert for monthly
        String monthlyResult = HearingReportEmailServiceConfig.monthlyReportAttachmentName("").apply(testDate);
        assertEquals("APRIL-2023.csv", monthlyResult);

        // Act & Assert for weekly
        String weeklyResult = HearingReportEmailServiceConfig.weeklyReportAttachmentName("").apply(testDate);
        assertEquals("2023-03-25.csv", weeklyResult);
    }
}


