package uk.gov.hmcts.reform.em.hrs.interceptors;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.em.hrs.exception.UnauthorisedServiceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class DeleteRequestInterceptorTest {

    private static final String AUTHORISED_SERVICE = "authorised_service";
    private static final String UNAUTHORISED_SERVICE = "unauthorised_service";
    private static final String SERVICE_AUTHORIZATION_HEADER = "ServiceAuthorization";
    private static final String TEST_S2S_TOKEN = "s2s-token";

    @Mock
    private AuthTokenValidator authTokenValidator;

    @InjectMocks
    private DeleteRequestInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        openMocks(this);
        setField(interceptor, "authorisedServices", List.of(AUTHORISED_SERVICE));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should succeed when service is authorised and token has Bearer prefix")
    void testPreHandleShouldSucceedForAuthorisedServiceWithBearerPrefix() {
        final String tokenWithPrefix = "Bearer " + TEST_S2S_TOKEN;
        request.addHeader(SERVICE_AUTHORIZATION_HEADER, tokenWithPrefix);
        when(authTokenValidator.getServiceName(tokenWithPrefix)).thenReturn(AUTHORISED_SERVICE);

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Should succeed when service is authorised and token is missing Bearer prefix")
    void testPreHandleShouldSucceedForAuthorisedServiceWithoutBearerPrefix() {
        request.addHeader(SERVICE_AUTHORIZATION_HEADER, TEST_S2S_TOKEN);
        final String expectedToken = "Bearer " + TEST_S2S_TOKEN;
        when(authTokenValidator.getServiceName(expectedToken)).thenReturn(AUTHORISED_SERVICE);

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Should succeed when service is authorised and S2S header is missing")
    void testPreHandleShouldSucceedForAuthorisedServiceWithMissingHeader() {
        when(authTokenValidator.getServiceName(null)).thenReturn(AUTHORISED_SERVICE);

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Should throw UnauthorisedServiceException for an unauthorised service")
    void testPreHandleShouldThrowExceptionForUnauthorisedService() {
        final String tokenWithPrefix = "Bearer " + TEST_S2S_TOKEN;
        request.addHeader(SERVICE_AUTHORIZATION_HEADER, tokenWithPrefix);
        when(authTokenValidator.getServiceName(tokenWithPrefix)).thenReturn(UNAUTHORISED_SERVICE);

        UnauthorisedServiceException exception = assertThrows(
            UnauthorisedServiceException.class,
            () -> interceptor.preHandle(request, response, null)
        );

        assertEquals(
            "Service " + UNAUTHORISED_SERVICE + " not in configured list for deleting recordings",
            exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should throw UnauthorisedServiceException when S2S header is missing and service is not authorised")
    void testPreHandleShouldThrowExceptionForMissingHeaderAndUnauthorisedService() {
        when(authTokenValidator.getServiceName(null)).thenReturn(UNAUTHORISED_SERVICE);

        UnauthorisedServiceException exception = assertThrows(
            UnauthorisedServiceException.class,
            () -> interceptor.preHandle(request, response, null)
        );

        assertEquals(
            "Service " + UNAUTHORISED_SERVICE + " not in configured list for deleting recordings",
            exception.getMessage()
        );
    }
}
