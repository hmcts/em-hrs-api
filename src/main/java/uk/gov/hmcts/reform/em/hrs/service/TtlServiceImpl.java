package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;

import java.time.LocalDate;
import java.time.Period;

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
        var now = LocalDate.now(EUROPE_LONDON_ZONE_ID);
        if (serviceCode != null) {
            Period ttlForService = ttlMapperConfig.getTtlServiceMap().get(serviceCode);
            if (ttlForService != null) {
                return now.plusDays(ttlForService.getDays());
            }
        }

        if (jurisdictionCode != null) {
            Period ttlForJurisdiction = ttlMapperConfig.getTtlJurisdictionMap().get(jurisdictionCode);
            if (ttlForJurisdiction != null) {
                return now.plusDays(ttlForJurisdiction.getDays());
            }
        }
        Period defaultTtl = ttlMapperConfig.getDefaultTTL();
        return now.plusDays(defaultTtl.getDays());
    }

}
