package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import static uk.gov.hmcts.reform.em.hrs.config.ClockConfig.EUROPE_LONDON_ZONE_ID;

@Service
public class TtlServiceImpl implements TtlService {

    private final boolean ttlEnabled;
    private final TTLMapperConfig ttlMapperConfig;

    public TtlServiceImpl(
        @Value("${ttl.enabled}") boolean ttlEnabled,
        TTLMapperConfig ttlMapperConfig) {
        this.ttlEnabled = ttlEnabled;
        this.ttlMapperConfig = ttlMapperConfig;
    }

    @Override
    public boolean isTtlEnabled() {
        return ttlEnabled;
    }

    public LocalDate createTtl(String serviceCode, String jurisdictionCode) {
        var ttlPeriod = Optional.ofNullable(serviceCode)
            .map(ttlMapperConfig.getTtlServiceMap()::get)
            .or(() -> Optional.ofNullable(jurisdictionCode)
                .map(ttlMapperConfig.getTtlJurisdictionMap()::get))
            .orElseGet(ttlMapperConfig::getDefaultTTL);
        return calculateTtl(ttlPeriod);
    }

    private LocalDate calculateTtl(Period ttl) {
        var now = LocalDate.now(EUROPE_LONDON_ZONE_ID);
        return now.plusYears(ttl.getYears()).plusMonths(ttl.getMonths()).plusDays(ttl.getDays());
    }
}
