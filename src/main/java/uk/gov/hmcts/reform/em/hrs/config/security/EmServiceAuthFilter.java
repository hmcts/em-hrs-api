package uk.gov.hmcts.reform.em.hrs.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EmServiceAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public static final String NOT_APPLICABLE = "N/A";

    private static final Logger LOG = LoggerFactory.getLogger(EmServiceAuthFilter.class);

    private final List<String> authorisedServices;

    private final AuthTokenValidator authTokenValidator;

    public EmServiceAuthFilter(AuthTokenValidator authTokenValidator, List<String> authorisedServices) {

        this.authTokenValidator = authTokenValidator;
        if (CollectionUtils.isEmpty(authorisedServices)) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }
        this.authorisedServices = authorisedServices.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        try {
            String bearerToken = extractBearerToken(request);
            String serviceName = authTokenValidator.getServiceName(bearerToken);
            if (!authorisedServices.contains(serviceName)) {
                LOG.info(
                    "service forbidden {} for endpoint: {} method: {} ",
                    serviceName,
                    request.getRequestURI(),
                    request.getMethod()
                );
                return null;
            } else {
                LOG.info(
                    "service authorized {} for endpoint: {} method: {}  ",
                    serviceName,
                    request.getRequestURI(),
                    request.getMethod()
                );

                return serviceName;
            }
        } catch (InvalidTokenException | ServiceException exception) {
            LOG.warn("Unsuccessful service authentication", exception);
            return null;
        }
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return NOT_APPLICABLE;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(SERVICE_AUTHORIZATION);
        if (Objects.isNull(token)) {
            throw new InvalidTokenException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
