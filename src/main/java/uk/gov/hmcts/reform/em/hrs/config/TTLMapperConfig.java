package uk.gov.hmcts.reform.em.hrs.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.util.Map;

@Component
@Data
@PropertySource(value = "classpath:ttl_service_map.json",
    factory  = TTLPropertySourceFactory.class)
@PropertySource(value = "classpath:ttl_jurisdiction_map.json",
    factory  = TTLPropertySourceFactory.class)
@ConfigurationProperties()
public class TTLMapperConfig {

    @Value("${ttl.default-ttl}")
    private Period defaultTTL;

    private Map<String, Period> ttlServiceMap;
    private Map<String, Period> ttlJurisdictionMap;
}
