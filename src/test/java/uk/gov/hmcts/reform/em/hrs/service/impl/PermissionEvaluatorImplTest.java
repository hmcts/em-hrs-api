package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorImplTest {

    private static final String ALLOWED_ROLE = "caseworker-hrs";
    private static final String FORBIDDEN_ROLE = "citizen";
    private static final String TOKEN_VALUE = "token-value";
    private static final String USER_EMAIL = "user@example.com";
    private static final UUID RECORDING_ID = UUID.randomUUID();

    @Mock
    private SecurityService securityService;

    @Mock
    private ShareesRepository shareesRepository;

    @Mock
    private AuditEntryService auditEntryService;

    @Mock
    private JwtAuthenticationToken authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PermissionEvaluatorImpl permissionEvaluator;

    @BeforeEach
    void setUp() {
        permissionEvaluator.allowedRoles = List.of(ALLOWED_ROLE);
    }

    @Test
    void hasPermissionWhenUserHasAllowedRoleShouldReturnTrue() {
        setupToken();
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(List.of(ALLOWED_ROLE, FORBIDDEN_ROLE));
        when(securityService.getUserInfo("Bearer " + TOKEN_VALUE)).thenReturn(userInfo);

        Object target = new Object();

        boolean result = permissionEvaluator.hasPermission(authentication, target, "READ");

        assertThat(result).isTrue();
        verifyNoInteractions(shareesRepository, auditEntryService);
    }

    @Test
    void hasPermissionWhenUserHasRolesButNoneAllowedShouldCheckSharees() {
        setupToken();
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(List.of(FORBIDDEN_ROLE));
        when(securityService.getUserInfo("Bearer " + TOKEN_VALUE)).thenReturn(userInfo);

        boolean result = permissionEvaluator.hasPermission(authentication, "NotASegment", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermissionWhenUserHasNoRolesAndTargetIsNotSegmentShouldReturnFalse() {
        setupToken();
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(Collections.emptyList());
        when(securityService.getUserInfo("Bearer " + TOKEN_VALUE)).thenReturn(userInfo);

        boolean result = permissionEvaluator.hasPermission(authentication, "NotASegment", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermissionWhenTargetIsSegmentAndUserIsShareeShouldReturnTrue() {
        setupToken();
        setupUserInfoWithNoAllowedRoles();

        HearingRecording recording = HearingRecording.builder().id(RECORDING_ID).build();
        HearingRecordingSegment segment = HearingRecordingSegment.builder()
            .hearingRecording(recording)
            .build();

        HearingRecordingSharee sharee = HearingRecordingSharee.builder()
            .hearingRecording(recording)
            .shareeEmail(USER_EMAIL)
            .build();

        when(securityService.getUserEmail("Bearer " + TOKEN_VALUE)).thenReturn(USER_EMAIL);
        when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(List.of(sharee));

        boolean result = permissionEvaluator.hasPermission(authentication, segment, "READ");

        assertThat(result).isTrue();
        verifyNoInteractions(auditEntryService);
    }

    @Test
    void hasPermissionWhenTargetIsSegmentButUserIsNotShareeShouldReturnFalseAndAudit() {
        setupToken();
        setupUserInfoWithNoAllowedRoles();

        HearingRecording recording = HearingRecording.builder().id(RECORDING_ID).build();
        HearingRecordingSegment segment = HearingRecordingSegment.builder()
            .hearingRecording(recording)
            .build();

        when(securityService.getUserEmail("Bearer " + TOKEN_VALUE)).thenReturn(USER_EMAIL);
        when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(Collections.emptyList());

        boolean result = permissionEvaluator.hasPermission(authentication, segment, "READ");

        assertThat(result).isFalse();
        verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_UNAUTHORIZED);
    }

    @Test
    void hasPermissionWhenTargetIsSegmentButUserIsSharedOnDifferentRecordingShouldReturnFalseAndAudit() {
        setupToken();
        setupUserInfoWithNoAllowedRoles();

        HearingRecording requestedRecording = HearingRecording.builder().id(RECORDING_ID).build();
        HearingRecordingSegment segment = HearingRecordingSegment.builder()
            .hearingRecording(requestedRecording)
            .build();

        HearingRecording differentRecording = HearingRecording.builder().id(UUID.randomUUID()).build();
        HearingRecordingSharee sharee = HearingRecordingSharee.builder()
            .hearingRecording(differentRecording)
            .shareeEmail(USER_EMAIL)
            .build();

        when(securityService.getUserEmail("Bearer " + TOKEN_VALUE)).thenReturn(USER_EMAIL);
        when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(List.of(sharee));

        boolean result = permissionEvaluator.hasPermission(authentication, segment, "READ");

        assertThat(result).isFalse();
        verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_UNAUTHORIZED);
    }

    @Test
    void hasPermissionSerializableOverloadShouldReturnFalse() {
        Authentication auth = mock(Authentication.class);
        Serializable id = UUID.randomUUID();

        boolean result = permissionEvaluator.hasPermission(auth, id, "ClassName", "READ");

        assertThat(result).isFalse();
    }

    private void setupToken() {
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn(TOKEN_VALUE);
    }

    private void setupUserInfoWithNoAllowedRoles() {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("user-uid");
        when(userInfo.getName()).thenReturn("User Name");
        when(userInfo.getRoles()).thenReturn(List.of(FORBIDDEN_ROLE));
        when(securityService.getUserInfo("Bearer " + TOKEN_VALUE)).thenReturn(userInfo);
    }
}
