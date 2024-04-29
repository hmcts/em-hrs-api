package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "hrs.allowed-roles.value=[caseworker-hrs-searcher]")
@SpringBootTest(classes = {PermissionEvaluatorImpl.class})
class PermissionEvaluatorImplTest {

    @MockBean
    private SecurityService securityService;

    @MockBean
    private ShareesRepository shareesRepository;

    @MockBean
    private AuditEntryService auditEntryService;

    private JwtAuthenticationToken authentication;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UserInfo HRS_SEARCHER_INFO = UserInfo.builder()
        .uid(USER_ID)
        .roles(List.of("caseworker-hrs-searcher"))
        .build();
    private static final UserInfo HRS_SHAREE_INFO = UserInfo.builder()
        .uid(USER_ID)
        .roles(List.of("caseworker"))
        .build();
    private static final UUID recordingId = UUID.randomUUID();
    private static final HearingRecordingSegment segment = HearingRecordingSegment.builder()
        .hearingRecording(HearingRecording.builder().id(recordingId).build())
        .build();
    private String shareeEmail = "sharee@sharee.com";
    private List<HearingRecordingSharee> hearingRecordingWithSharee;
    private List<HearingRecordingSharee> hearingRecordingWithNoSharees = new ArrayList<>();

    @Autowired
    PermissionEvaluatorImpl permissionEvaluator;

    @BeforeEach
    public void setup() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user")
            .build();
        Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_read");
        authentication = new JwtAuthenticationToken(jwt, authorities);

        HearingRecordingSharee hearingRecordingSharee = new HearingRecordingSharee();
        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setId(recordingId);
        hearingRecordingSharee.setHearingRecording(hearingRecording);
        hearingRecordingWithSharee = List.of(hearingRecordingSharee);
        setRolesAllowedToDownloadByDefaultToBeCaseworkerHrsSearcher();

    }

    @Test
    void testPermissionOnDownloadWithSearcherRoleSuccess() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_SEARCHER_INFO);
        assertTrue(permissionEvaluator.hasPermission(authentication, null, "READ"));
    }


    @Test
    void testPermissionOnDownloadWithCaseWorkerRoleAndNoEmailShareGrantFailure() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_SHAREE_INFO);
        assertFalse(permissionEvaluator.hasPermission(authentication, null, "READ"));
    }

    @Test
    void testPermissionOnDownloadShareeSuccess() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_SHAREE_INFO);
        when(securityService.getUserEmail(Mockito.anyString())).thenReturn(shareeEmail);
        when(shareesRepository.findByShareeEmailIgnoreCase(Mockito.anyString())).thenReturn(hearingRecordingWithSharee);
        assertTrue(permissionEvaluator.hasPermission(authentication, segment, "READ"));
    }

    @Test
    void testPermissionOnDownloadShareeFailure() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_SHAREE_INFO);
        when(securityService.getUserEmail(Mockito.anyString())).thenReturn(shareeEmail);
        when(shareesRepository.findByShareeEmailIgnoreCase(Mockito.anyString()))
            .thenReturn(hearingRecordingWithNoSharees);
        boolean permissionResult = permissionEvaluator.hasPermission(authentication, segment, "READ");
        assertFalse(permissionResult);
        verify(auditEntryService, times(1)).createAndSaveEntry(
            any(HearingRecordingSegment.class),
            any(AuditActions.class)
        );
    }

    @Test
    void testPermissionOnDownloadFailure() {
        assertFalse(permissionEvaluator.hasPermission(
            authentication,
            null,
            "uk.gov.hmcts.reform.em.hrs.service"
                + ".SegmentDownloadServiceImpl",
            "READ"
        ));
    }


    private void setRolesAllowedToDownloadByDefaultToBeCaseworkerHrsSearcher() {
        ReflectionTestUtils.setField(permissionEvaluator, "allowedRoles", List.of("caseworker-hrs-searcher"));
    }

}
