package uk.gov.hmcts.reform.em.hrs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;

import java.time.LocalDate;
import java.time.Period;

import static uk.gov.hmcts.reform.em.hrs.config.ClockConfig.EUROPE_LONDON_ZONE_ID;

@Service
public class TtlServiceImpl implements TtlService {

    private static final Logger LOGGER =  LoggerFactory.getLogger(TtlServiceImpl.class);

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
        Period ttlPeriod = null;
        if (serviceCode != null) {
            ttlPeriod = ttlMapperConfig.getTtlServiceMap().get(serviceCode);
        }

        if (ttlPeriod == null && jurisdictionCode != null) {
            ttlPeriod = ttlMapperConfig.getTtlJurisdictionMap().get(jurisdictionCode);
        }

        if (ttlPeriod == null) {
            ttlPeriod = ttlMapperConfig.getDefaultTTL();
        }
        var ttlDate = calculateTtl(ttlPeriod);
        LOGGER.info("Found ttl period {}, TtlDate {}", ttlPeriod, ttlDate);
        return ttlDate;
    }

    private LocalDate calculateTtl(Period ttl) {
        var now = LocalDate.now(EUROPE_LONDON_ZONE_ID);
        return now.plusYears(ttl.getYears()).plusMonths(ttl.getMonths()).plusDays(ttl.getDays());
    }
}
