package uk.gov.hmcts.reform.em.hrs.cftlib;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CftLibConfig implements CFTLibConfigurer {

    private static final String ROLE_CITIZEN = "citizen";
    private static final String ROLE_CASEWORKER = "caseworker";
    private static final String ROLE_CASEWORKER_HRS = "caseworker-hrs";
    private static final String ROLE_CASEWORKER_HRS_SEARCHER = "caseworker-hrs-searcher";
    private static final String ROLE_CFT_TTL_MANAGER = "cft-ttl-manager";
    private static final String ROLE_CASEWORKER_HRS_SYSTEMUPDATE = "caseworker-hrs-systemupdate";
    private static final String TEST_SEARCHER_EMAIL = "em-test-searcher@test.hmcts.net";
    private static final String TEST_REQUESTOR_EMAIL = "em-test-requestor@test.hmcts.net";
    private static final String TEST_CITIZEN_EMAIL = "em-test-citizen@test.hmcts.net";
    private static final String HRS_TESTER_EMAIL = "hrs.tester@hmcts.net";
    private static final String HRS_FUNC_SYS_USER_EMAIL = "hrs.functional.system.user@hmcts.net";
    private static final String DATA_STORE_IDAM_USER_EMAIL = "data.store.idam.system.user@gmail.com";
    private static final String HRS = "HRS";
    private static final String HEARING_RECORDINGS = "HearingRecordings";
    private static final String PROFILE_STATUS = "1_OPENED";
    private static final String PASSWORD = "password";

    private final IdamClient idamClient;

    public CftLibConfig(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Override
    public void configure(CFTLib lib) throws Exception {
        setupUsers(lib);

        setupRoleAssignments(lib);

        lib.importDefinition(Files.readAllBytes(
            Path.of("src/functionalTest/resources/CCD_HRS_v1.7-AAT.xlsx")));

    }

    private void setupUsers(CFTLib lib) {
        for (String p : List.of(
            TEST_SEARCHER_EMAIL,
            TEST_REQUESTOR_EMAIL,
            TEST_CITIZEN_EMAIL,
            HRS_TESTER_EMAIL,
            HRS_FUNC_SYS_USER_EMAIL)) {
            lib.createProfile(p, HRS, HEARING_RECORDINGS, PROFILE_STATUS);
        }

        lib.createRoles(
            ROLE_CITIZEN,
            ROLE_CASEWORKER,
            ROLE_CASEWORKER_HRS,
            ROLE_CASEWORKER_HRS_SEARCHER,
            ROLE_CFT_TTL_MANAGER,
            ROLE_CASEWORKER_HRS_SYSTEMUPDATE
        );

        lib.createIdamUser(DATA_STORE_IDAM_USER_EMAIL, ROLE_CASEWORKER);

        lib.createIdamUser(HRS_TESTER_EMAIL,
                           ROLE_CITIZEN,
                           ROLE_CASEWORKER,
                           ROLE_CASEWORKER_HRS,
                           ROLE_CASEWORKER_HRS_SEARCHER,
                           ROLE_CFT_TTL_MANAGER,
                           ROLE_CASEWORKER_HRS_SYSTEMUPDATE);

        lib.createIdamUser(TEST_SEARCHER_EMAIL,
                           ROLE_CITIZEN,
                           ROLE_CASEWORKER,
                           ROLE_CASEWORKER_HRS,
                           ROLE_CASEWORKER_HRS_SEARCHER);

        lib.createIdamUser(TEST_REQUESTOR_EMAIL,
                           ROLE_CITIZEN,
                           ROLE_CASEWORKER);

        lib.createIdamUser(TEST_CITIZEN_EMAIL,
                           ROLE_CITIZEN);
    }

    private void setupRoleAssignments(CFTLib lib) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        String assignments = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                                  .getInputStream(), Charset.defaultCharset());

        String token = idamClient.getAccessToken(TEST_SEARCHER_EMAIL, PASSWORD);
        UserInfo userInfo = idamClient.getUserInfo(token);
        String updated = assignments.replace("Searcher IDAM ID", userInfo.getUid());

        lib.configureRoleAssignments(updated);
    }
}
