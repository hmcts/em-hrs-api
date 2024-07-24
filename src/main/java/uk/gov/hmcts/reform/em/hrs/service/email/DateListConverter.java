package uk.gov.hmcts.reform.em.hrs.service.email;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DateListConverter {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<LocalDate> convert(String source) {
        if (source == null || source.trim().equals("")) {
            return new ArrayList<>();
        }
        return Arrays.stream(source.split(","))
            .map(date -> LocalDate.parse(date.trim(), formatter))
            .collect(Collectors.toList());
    }
}
