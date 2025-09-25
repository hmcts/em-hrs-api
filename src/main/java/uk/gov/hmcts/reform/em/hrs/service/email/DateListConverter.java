package uk.gov.hmcts.reform.em.hrs.service.email;

import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DateListConverter {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<LocalDate> convert(String source) {
        if (StringUtils.isEmpty(source)) {
            return new ArrayList<>();
        }
        return Arrays.stream(source.split(","))
            .map(date -> LocalDate.parse(date.trim(), formatter))
            .toList();
    }
}
