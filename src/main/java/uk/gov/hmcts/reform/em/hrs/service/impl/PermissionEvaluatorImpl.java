package uk.gov.hmcts.reform.em.hrs.service.impl;


import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionEvaluatorImpl.class);

    @Value("#{'${hrs.allowed-roles}'.split(',')}")
    List<String> allowedRoles;

    private final SecurityService securityService;

    private final ShareesRepository shareesRepository;

    private final AuditEntryService auditEntryService;

    @Autowired
    public PermissionEvaluatorImpl(SecurityService securityService, ShareesRepository shareesRepository,
                                   AuditEntryService auditEntryService) {
        this.securityService = securityService;
        this.shareesRepository = shareesRepository;
        this.auditEntryService = auditEntryService;
    }

    @Override
    public boolean hasPermission(@NotNull Authentication authenticationToken,
                                 @NotNull Object segment,
                                 @NotNull Object permissionString) {

        var jwtAuthenticationToken = (JwtAuthenticationToken) authenticationToken;
        String token = "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();

        var userInfo = securityService.getUserInfo(token);

        LOGGER.info("********************************");
        LOGGER.info("SECURITY FILTER");
        LOGGER.info("********************************");
        LOGGER.info(
            "User ({}:{}) with roles ({}) attempting to access recording",
            userInfo.getUid(),
            userInfo.getName(),
            userInfo.getRoles()
        );

        if (!isEmpty(userInfo.getRoles())) {
            Optional<String> userRole = userInfo.getRoles().stream()
                .filter(role -> allowedRoles.contains(role))
                .findFirst();
            if (userRole.isPresent()) {
                LOGGER.info("User granted access with allowed role ({})", userRole);
                return true;
            }
        }

        if (segment instanceof HearingRecordingSegment hrSegment) {
            HearingRecording hr = hrSegment.getHearingRecording();
            UUID recordingId = hr.getId();
            String shareeEmail = securityService.getUserEmail(token);
            LOGGER.info(
                "User attempting to access recording ref ({}) with email ({})",
                hr.getRecordingRef(),
                shareeEmail
            );
            List<HearingRecordingSharee> sharedRecordings = shareesRepository.findByShareeEmailIgnoreCase(shareeEmail);
            LOGGER.info(
                "recordings that are shared with the user: ({}) are ({})", shareeEmail,
                sharedRecordings.stream()
                    .map(sharedRecording -> sharedRecording.getHearingRecording().getCaseRef())
                    .toList()
            );
            if (!isEmpty(sharedRecordings)) {
                Optional<HearingRecordingSharee> hearingRecording = sharedRecordings.stream()
                    .filter(sharedRecording -> sharedRecording.getHearingRecording().getId().equals(recordingId))
                    .findFirst();
                if (hearingRecording.isPresent()) {
                    LOGGER.info("User granted access through shared email ({})", shareeEmail);
                    return true;
                }
            }
            auditEntryService.createAndSaveEntry(hrSegment, AuditActions.USER_DOWNLOAD_UNAUTHORIZED);
        }
        return false;//Not a segment
    }

    @Override
    public boolean hasPermission(@NotNull Authentication authentication,
                                 @NotNull Serializable serializable,
                                 @NotNull String className,
                                 @NotNull Object permissions) {
        LOGGER.error("Should not be invoking this method");
        return false;
    }
}
