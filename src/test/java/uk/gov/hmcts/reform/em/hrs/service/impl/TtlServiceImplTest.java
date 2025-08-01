package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TtlServiceImplTest {

    private TTLMapperConfig ttlMapperConfig = mock(TTLMapperConfig.class);

    private TtlServiceImpl ttlServiceImpl =  new TtlServiceImpl(ttlMapperConfig);

    @Test
    void shouldReturnTtlForServiceCode() {
        // Given
        String serviceCode = "SERVICEA";
        Period periodForService = Period.ofDays(10);
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of(serviceCode, periodForService));

        // When
        LocalDate ttlDate = ttlServiceImpl.createTtl(serviceCode, null, LocalDate.now());

        // Then
        LocalDate now = LocalDate.now();
        assertEquals(now.plusDays(10), ttlDate);
        verify(ttlMapperConfig).getTtlServiceMap();
    }

    @Test
    void shouldCheckUpperCaseForCodes() {
        // Given
        String serviceCode = "servicea";
        Period periodForService = Period.ofDays(10);
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of("SERVICEA", periodForService));

        // When
        LocalDate ttlDate = ttlServiceImpl.createTtl(serviceCode, null, LocalDate.now());

        // Then
        LocalDate now = LocalDate.now();
        assertEquals(now.plusDays(10), ttlDate);
        verify(ttlMapperConfig).getTtlServiceMap();
    }

    @Test
    void shouldReturnTtlForJurisdictionCodeWhenServiceCodeNotFound() {
        // Given
        String jurisdictionCode = "jurisDictionA";
        Period periodForJurisdiction = Period.ofDays(15);
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of("JURISDICTIONA", periodForJurisdiction));

        // When
        LocalDate ttlDate = ttlServiceImpl.createTtl("notFound_service", jurisdictionCode, LocalDate.now());

        // Then
        LocalDate now = LocalDate.now();
        assertEquals(now.plusDays(15), ttlDate);
        verify(ttlMapperConfig).getTtlJurisdictionMap();
    }

    @Test
    void shouldReturnDefaultTtlWhenNeitherServiceNorJurisdictionCodeFound() {
        // Given
        String serviceCode = "serviceA";
        Period periodForService = Period.ofDays(10);
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of(serviceCode, periodForService));

        String jurisdictionCode = "jurisdictionA";
        Period periodForJurisdiction = Period.ofDays(15);
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of(jurisdictionCode, periodForJurisdiction));

        Period defaultTtl = Period.ofDays(30);
        when(ttlMapperConfig.getDefaultTTL()).thenReturn(defaultTtl);

        // When
        LocalDate ttlDate = ttlServiceImpl.createTtl("notFound_service", "notFound_jurisdiction", LocalDate.now());

        // Then
        LocalDate now = LocalDate.now();
        assertEquals(now.plusDays(30), ttlDate);
        verify(ttlMapperConfig).getDefaultTTL();
    }
  
    @Test
    void shouldReturnDefaultTtlWhenServiceAndJurisdictionCodeNull() {
        // Given
        String serviceCode = "serviceA";
        Period periodForService = Period.ofDays(10);
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of(serviceCode, periodForService));

        String jurisdictionCode = "jurisdictionA";
        Period periodForJurisdiction = Period.ofDays(15);
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of(jurisdictionCode, periodForJurisdiction));

        Period defaultTtl = Period.ofDays(30);
        when(ttlMapperConfig.getDefaultTTL()).thenReturn(defaultTtl);

        // When
        LocalDate ttlDate = ttlServiceImpl.createTtl(null, null, LocalDate.now());

        // Then
        LocalDate now = LocalDate.now();
        assertEquals(now.plusDays(30), ttlDate);
        verify(ttlMapperConfig).getDefaultTTL();
    }

    @Test
    void testHasTtlConfigWithValidServiceCode() {
        String serviceCode = "SERVICE1";
        Period periodForService = Period.ofDays(10);
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of(serviceCode, periodForService));

        assertEquals(ttlServiceImpl.hasTtlConfig(serviceCode, null), "Yes");
    }

    @Test
    void testHasTtlConfigWithValidJurisdictionCode() {
        String jurisdictionCode = "JURISDICTION1";
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of(jurisdictionCode, Period.ofDays(10)));

        assertEquals(ttlServiceImpl.hasTtlConfig(null, jurisdictionCode), "Yes");
    }

    @Test
    void testHasTtlConfigWithInvalidCodes() {
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of());
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of("JRS", Period.ofDays(10)));

        assertEquals(ttlServiceImpl.hasTtlConfig("INVALID", "INVALID"),"No");
    }

    @Test
    void testHasTtlConfigWithBothValidCodes() {
        String serviceCode = "SERVICE1";
        String jurisdictionCode = "JURISDICTION1";
        when(ttlMapperConfig.getTtlServiceMap()).thenReturn(Map.of(serviceCode, Period.ofDays(10)));
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of(jurisdictionCode, Period.ofDays(10)));

        assertEquals(ttlServiceImpl.hasTtlConfig(serviceCode, jurisdictionCode),"Yes");
    }

}
