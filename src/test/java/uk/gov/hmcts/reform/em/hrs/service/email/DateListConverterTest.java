package uk.gov.hmcts.reform.em.hrs.service.email;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateListConverterTest {

    private DateListConverter dateListConverter = new DateListConverter();

    @Test
    public void testConvertValidDates() {
        String dates = "2024-07-19,2024-08-19,2024-09-19";
        List<LocalDate> expectedDates = List.of(
            LocalDate.of(2024, 7, 19),
            LocalDate.of(2024, 8, 19),
            LocalDate.of(2024, 9, 19)
        );

        List<LocalDate> actualDates = dateListConverter.convert(dates);

        assertEquals(expectedDates, actualDates);
    }

    @Test
    public void testConvertWithSpaces() {
        String dates = "2024-07-19, 2024-08-19 , 2024-09-19 ";
        List<LocalDate> expectedDates = List.of(
            LocalDate.of(2024, 7, 19),
            LocalDate.of(2024, 8, 19),
            LocalDate.of(2024, 9, 19)
        );

        List<LocalDate> actualDates = dateListConverter.convert(dates);

        assertEquals(expectedDates, actualDates);
    }

    @Test
    public void testConvertEmptyString() {
        String dates = "";
        List<LocalDate> actualDates = dateListConverter.convert(dates);
        assertEquals(List.of(), actualDates);
    }

    @Test
    public void testConvertNullString() {
        String dates = null;
        List<LocalDate> actualDates = dateListConverter.convert(dates);
        assertEquals(List.of(), actualDates);
    }

    @Test
    public void testConvertInvalidDate() {
        String dates = "2024-07-19,invalid-date,2024-09-19";
        try {
            dateListConverter.convert(dates);
        } catch (Exception e) {
            assertEquals(java.time.format.DateTimeParseException.class, e.getClass());
        }
    }
}
