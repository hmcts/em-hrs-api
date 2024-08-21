package uk.gov.hmcts.reform.em.hrs.interceptors;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.em.hrs.exception.UnauthorisedServiceException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class DeleteRequestInterceptor implements HandlerInterceptor {

    private final AuthTokenValidator tokenValidator;

    @Value("#{'${authorisation.deleteCase.s2s-names-whitelist}'.split(',')}")
    private List<String> authorisedServices;

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public DeleteRequestInterceptor(@Lazy AuthTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String serviceAuthToken = request.getHeader(SERVICE_AUTHORIZATION);
        String serviceName;
        if (Objects.nonNull(serviceAuthToken) && !serviceAuthToken.contains("Bearer")) {
            serviceName = tokenValidator.getServiceName("Bearer " + serviceAuthToken);
        } else {
            serviceName = tokenValidator.getServiceName(serviceAuthToken);
        }

        if (!authorisedServices.contains(serviceName)) {
            log.error("Service {} not allowed to delete recordings ", serviceName);
            throw new UnauthorisedServiceException(
                "Service " + serviceName + " not in configured list for deleting recordings");
        }
        return true;
    }
}
