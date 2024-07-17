package uk.gov.hmcts.reform.em.hrs.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.helper.AzureIntegrationTestOperations;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClientImpl;

import java.time.Period;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    TestAzureStorageConfig.class,
    BlobstoreClientImpl.class,
    AzureIntegrationTestOperations.class
})
@EnableConfigurationProperties(value = TTLMapperConfig.class)
@TestPropertySources({
    @TestPropertySource(value = "classpath:ttl_jurisdiction_map.json",
        factory  = TTLPropertySourceFactory.class),
    @TestPropertySource(value = "classpath:ttl_service_map.json",
        factory  = TTLPropertySourceFactory.class),
})
class TTLMapperConfigTest {

    @Autowired
    private TTLMapperConfig ttlMapperConfig;

    @Test
    void serviceMapIsSet() {
        Map<String, Period> ttlServiceMap = ttlMapperConfig.getTtlServiceMap();
        assertEquals(Period.of(1,2,3), ttlServiceMap.get("Testing123"));
        assertEquals(Period.of(2,3,4), ttlServiceMap.get("Testing12345"));
    }

    @Test
    void jurisdictionMapIsSet() {
        Map<String, Period> ttlJurisdictionMap = ttlMapperConfig.getTtlJurisdictionMap();
        assertEquals(Period.of(0,2, 3), ttlJurisdictionMap.get("Test"));
        assertEquals(Period.ofYears(1), ttlJurisdictionMap.get("Test1"));
    }
}
