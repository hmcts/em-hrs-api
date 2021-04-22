package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.ServiceDetails;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilServiceTest {

    @InjectMocks
    SecurityUtilService securityUtilService;

    @Test
    void testSuccessfulRetrievalOfUsernameFromSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        ServiceDetails serviceDetails = mock(ServiceDetails.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(serviceDetails);
        when(serviceDetails.getUsername()).thenReturn("x");

        SecurityContextHolder.setContext(securityContext);

        Assertions.assertEquals("x", securityUtilService.getCurrentlyAuthenticatedServiceName());
    }

    @Test
    void testFailureOfUsernameFromSecurityContextWhenItsNotThere() {
        Assertions.assertNull(securityUtilService.getCurrentlyAuthenticatedServiceName());
    }

}
