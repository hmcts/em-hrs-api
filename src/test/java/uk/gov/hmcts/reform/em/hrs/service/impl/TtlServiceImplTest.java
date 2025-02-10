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

    private TtlServiceImpl ttlServiceImpl =  new TtlServiceImpl(true, ttlMapperConfig);


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
    void shouldReturnTtlDisabled() {
        //given
        TtlServiceImpl ttlServiceDisabled = new TtlServiceImpl(false, ttlMapperConfig);

        // When
        boolean ttlEnabled = ttlServiceDisabled.isTtlEnabled();

        // Then
        assertEquals(false, ttlEnabled);
    }

    @Test
    void shouldReturnTtlEnabled() {
        // When
        boolean ttlEnabled = ttlServiceImpl.isTtlEnabled();

        // Then
        assertEquals(true, ttlEnabled);
    }
}
