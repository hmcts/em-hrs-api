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
        String serviceCode = "serviceA";
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
    void shouldReturnTtlForJurisdictionCodeWhenServiceCodeNotFound() {
        // Given
        String jurisdictionCode = "jurisdictionA";
        Period periodForJurisdiction = Period.ofDays(15);
        when(ttlMapperConfig.getTtlJurisdictionMap()).thenReturn(Map.of(jurisdictionCode, periodForJurisdiction));

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
}
