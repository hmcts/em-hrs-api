package uk.gov.hmcts.reform.em.hrs.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class DeleteRequestInterceptorTest {

    @Mock
    private AuthTokenValidator authTokenValidator;

    @InjectMocks
    private DeleteRequestInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        try (AutoCloseable ignored = openMocks(this)) {
            setField(interceptor, "authorisedServices", List.of("Authorised Service"));
        }
    }

    @Test
    void testAuthorisedService() {

        doReturn("Authorised Service").when(authTokenValidator).getServiceName(any());

        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        HttpServletResponse httpServletResponse = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(httpServletRequest, httpServletResponse, null);
        assertTrue(result);
        assertEquals(200, httpServletResponse.getStatus());
    }


    @Test
    void testUnauthorisedService() {

        doReturn("").when(authTokenValidator).getServiceName(any());

        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        HttpServletResponse httpServletResponse = new MockHttpServletResponse();

        assertThrows(UnauthorisedServiceException.class,
                     () -> interceptor.preHandle(httpServletRequest, httpServletResponse, null));
    }
}
