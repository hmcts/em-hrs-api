package uk.gov.hmcts.reform.em.hrs.cftlib;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CftLibConfig implements CFTLibConfigurer {

    @Override
    public void configure(CFTLib lib) throws Exception {
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
            "caseworker-hrs-searcher"
        );

        lib.createIdamUser("data.store.idam.system.user@gmail.com","caseworker");

        lib.createIdamUser("hrs.tester@hmcts.net",
                           "citizen",
                           "caseworker",
                           "caseworker-hrs",
                           "caseworker-hrs-searcher");

        lib.createIdamUser("em-test-searcher@test.hmcts.net",
                           "citizen",
                           "caseworker",
                           "caseworker-hrs",
                           "caseworker-hrs-searcher");

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                        .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);

        lib.importDefinition(Files.readAllBytes(
            Path.of("src/functionalTest/resources/CCD_HRS_v1.2-AAT.xlsx")));

    }
}
