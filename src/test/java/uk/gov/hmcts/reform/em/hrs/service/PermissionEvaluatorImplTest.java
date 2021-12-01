package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.Assert;
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
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@TestPropertySource(properties = "hrs.allowed-roles.value=[caseworker-hrs-searcher]")
@SpringBootTest(classes = {PermissionEvaluatorImpl.class})
public class PermissionEvaluatorImplTest {

    @MockBean
    private SecurityService securityService;

    @MockBean
    private ShareesRepository shareesRepository;

    private JwtAuthenticationToken authentication;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UserInfo HRS_USER_INFO = UserInfo.builder()
        .uid(USER_ID)
        .roles(Arrays.asList("caseworker-hrs-searcher"))
        .build();
    private static final UserInfo NON_HRS_USER_INFO = UserInfo.builder()
        .uid(USER_ID)
        .roles(Arrays.asList("sharee"))
        .build();
    private static final UUID recordingId = UUID.randomUUID();
    private static final HearingRecordingSegment segment = HearingRecordingSegment.builder()
        .hearingRecording(HearingRecording.builder().id(recordingId).build())
        .build();
    private String shareeEmail = "sharee@sharee.com";
    private List<HearingRecordingSharee> hearingRecordingSharees;

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
        hearingRecordingSharees = Arrays.asList(hearingRecordingSharee);
    }

    @Test
    public void testPermissionOnDownloadCaseWorkerSuccess() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_USER_INFO);
        ReflectionTestUtils.setField(permissionEvaluator, "allowedRoles", Arrays.asList("caseworker-hrs-searcher"));

        Assert.assertTrue(permissionEvaluator.hasPermission(authentication, null, "READ"));
    }

    @Test
    public void testPermissionOnDownloadCaseWorkerFailure() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(HRS_USER_INFO);
        ReflectionTestUtils.setField(permissionEvaluator, "allowedRoles", Arrays.asList("caseworker"));

        Assert.assertFalse(permissionEvaluator.hasPermission(authentication, null, "READ"));
    }

    @Test
    public void testPermissionOnDownloadShareeSuccess() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(NON_HRS_USER_INFO);
        ReflectionTestUtils.setField(permissionEvaluator, "allowedRoles", Arrays.asList("caseworker-hrs-searcher"));
        when(securityService.getUserEmail(Mockito.anyString())).thenReturn(shareeEmail);
        when(shareesRepository.findByShareeEmail(Mockito.anyString())).thenReturn(hearingRecordingSharees);
        Assert.assertTrue(permissionEvaluator.hasPermission(authentication, segment, "READ"));
    }

    @Test
    public void testPermissionOnDownloadShareeFailure() {
        when(securityService.getUserInfo(Mockito.anyString())).thenReturn(NON_HRS_USER_INFO);
        ReflectionTestUtils.setField(permissionEvaluator, "allowedRoles", Arrays.asList("caseworker-hrs-searcher"));
        when(securityService.getUserEmail(Mockito.anyString())).thenReturn(shareeEmail);
        when(shareesRepository.findByShareeEmail(Mockito.anyString())).thenReturn(hearingRecordingSharees);
        Assert.assertFalse(permissionEvaluator.hasPermission(authentication, null, "READ"));
    }

    @Test
    public void testPermissionOnDownloadFailure() {
        Assert.assertFalse(permissionEvaluator.hasPermission(authentication, null,
            "uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadServiceImpl","READ"));
    }
}
