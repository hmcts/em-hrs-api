package uk.gov.hmcts.reform.em.hrs.auditlog;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;
import uk.gov.hmcts.reform.em.hrs.model.LogOnlyAuditEntry;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditLogFormatterTest {

    private final AuditLogFormatter alf = new AuditLogFormatter();

    //including ID - here to catch if class is extended, but not formatter
    private final int numberOfFieldsInAuditEntryClass = 7;

    @Test
    void shouldFormatAuditEntryWithNoValuesPopulated() {
        AuditEntry entry = new LogOnlyAuditEntry();
        String result = alf.format(entry);
        System.out.println("Log Format=" + result);
        assertNotNull(result);
    }

    @Test
    void shouldFormatAuditEntryWithAllValuesPopulated() throws ParseException {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        Date now = format.parse("9/06/2021 08:52:52.422");
        AuditEntry entry = new LogOnlyAuditEntry();

        entry.setIpAddress("ip");
        entry.setCaseId(1234567890123456789L);
        entry.setUsername("userName@hmcts.net.internal");
        entry.setEventDateTime(now);
        entry.setServiceName("SUT");
        entry.setAction(AuditActions.USER_DOWNLOAD_OK);

        String result = alf.format(entry);
        assertEquals(numberOfFieldsInAuditEntryClass, AuditEntry.class.getDeclaredFields().length);
        System.out.println("Log Format=" + result);
        assertEquals("HRS-API dateTime:2021-06-09T08:52:52.422,"
            + "action:USER_DOWNLOAD_OK,"
            + "clientIp:ip,"
            + "service:SUT,"
            + "user:userName@hmcts.net.internal,"
            + "caseId:1234567890123456789", result);
    }

    @Test
    void shouldTruncateMillisecondsFromDateWhenEqualToZero() throws ParseException {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        Date now = format.parse("9/06/2021 08:52:52.000");
        AuditEntry entry = new LogOnlyAuditEntry();

        entry.setEventDateTime(now);
        String result = alf.format(entry);
        System.out.println("Log Format=" + result);
        assertEquals("HRS-API dateTime:2021-06-09T08:52:52", result);
    }


}
