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

    private final IdamClient idamClient;

    public CftLibConfig(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Override
    public void configure(CFTLib lib) throws Exception {
        setupUsers(lib);

        setupRoleAssignments(lib);

        lib.importDefinition(Files.readAllBytes(
            Path.of("src/functionalTest/resources/CCD_HRS_v1.3-AAT.xlsx")));

    }

    private void setupUsers(CFTLib lib) {
        for (String p : List.of(
            "em-test-searcher@test.hmcts.net",
            "em-test-requestor@test.hmcts.net",
            "em-test-citizen@test.hmcts.net",
            "hrs.tester@hmcts.net",
            "hrs.functional.system.user@hmcts.net")) {
            lib.createProfile(p, "HRS", "HearingRecordings", "1_OPENED");
        }

        lib.createRoles(
            "citizen",
            "caseworker",
            "caseworker-hrs",
            "caseworker-hrs-searcher",
            "cft-ttl-manager"
        );

        lib.createIdamUser("data.store.idam.system.user@gmail.com", "caseworker");

        lib.createIdamUser("hrs.tester@hmcts.net",
                           "citizen",
                           "caseworker",
                           "caseworker-hrs",
                           "caseworker-hrs-searcher",
                           "cft-ttl-manager");

        lib.createIdamUser("em-test-searcher@test.hmcts.net",
                           "citizen",
                           "caseworker",
                           "caseworker-hrs",
                           "caseworker-hrs-searcher");

        lib.createIdamUser("em-test-requestor@test.hmcts.net",
                           "citizen",
                           "caseworker");

        lib.createIdamUser("em-test-citizen@test.hmcts.net",
                           "citizen");
    }

    private void setupRoleAssignments(CFTLib lib) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        String assignments = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                                  .getInputStream(), Charset.defaultCharset());

        String token = idamClient.getAccessToken("em-test-searcher@test.hmcts.net", "password");
        UserInfo userInfo = idamClient.getUserInfo(token);
        String updated = assignments.replace("Searcher IDAM ID", userInfo.getUid());

        lib.configureRoleAssignments(updated);
    }
}
