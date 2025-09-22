package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {


    private SecurityService securityService;

    @Autowired
    public AuditorAwareImpl(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return Optional.ofNullable(authentication.getName());
        }
        return Optional.ofNullable(securityService.getAuditUserEmail());
    }

}
