package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Service
public class TtlServiceImpl implements TtlService {

    private final TTLMapperConfig ttlMapperConfig;

    private static final org.slf4j.Logger LOGGER =  LoggerFactory.getLogger(TtlServiceImpl.class);

    public TtlServiceImpl(
        TTLMapperConfig ttlMapperConfig) {
        this.ttlMapperConfig = ttlMapperConfig;
    }

    public LocalDate createTtl(String serviceCode, String jurisdictionCode, LocalDate createdDate) {
        var ttlPeriod = Optional.ofNullable(serviceCode)
            .map(code -> ttlMapperConfig.getTtlServiceMap().get(code.toUpperCase()))
            .or(() -> Optional.ofNullable(jurisdictionCode)
                .map(code -> ttlMapperConfig.getTtlJurisdictionMap().get(code.toUpperCase())))
            .orElseGet(() -> {
                LOGGER.info("Missing Service Code : {} and Jurisdiction Id : {}", serviceCode, jurisdictionCode);
                return ttlMapperConfig.getDefaultTTL();
            });
        return calculateTtl(ttlPeriod, createdDate);
    }

    private LocalDate calculateTtl(Period ttl, LocalDate createdDate) {
        return createdDate.plusYears(ttl.getYears()).plusMonths(ttl.getMonths()).plusDays(ttl.getDays());
    }
}
