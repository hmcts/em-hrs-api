package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.test.idam.IdamConfiguration;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = {
    ExtendedCcdHelper.class,
    CcdAuthTokenGeneratorConfiguration.class,
    AuthTokenGeneratorConfiguration.class,
    IdamConfiguration.class,
    EmTestConfig.class
})
@EnableAutoConfiguration
@ComponentScan(basePackages = {
    "uk.gov.hmcts.reform.em.test",
    "uk.gov.hmcts.reform.document"
})
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.yml")
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {
    private static final String MESSAGE = "Welcome to the HRS API!";

    private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTest.class);

    public static String SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION =
        "hrs.functional.system.user@hmcts.net";

    public static List<String>
        SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION_ROLES =
        List.of("caseworker", "caseworker-hrs", "caseworker-hrs-searcher", "ccd-import", "caseworker-hrs-systemupdate");

    protected static final String USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS = "em-test-searcher@test.hmcts.net";
    protected static final String USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY = "em-test-requestor@test.hmcts.net";
    protected static final String USER_WITH_NONACCESS_ROLE__CITIZEN = "em-test-citizen@test.hmcts.net";
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_SEARCHER_ROLE =
        List.of("caseworker", "caseworker-hrs", "caseworker-hrs-searcher");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");
    static int createUsersBaseTestRunCount = 0;

    @Value("${test.url}")
    private String testUrl;

    @Value("${upload-cdd-definition}")
    protected boolean uploadCcdDefinition;
    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;
    @Autowired
    protected IdamHelper idamHelper;

    @PostConstruct
    public void init() {
        int maxRuns = 1;

        if (uploadCcdDefinition && createUsersBaseTestRunCount < maxRuns) {

            LOGGER.info("BASE TEST POST CONSTRUCT INITIALISATIONS....");
            SerenityRest.useRelaxedHTTPSValidation();


            LOGGER.info("CREATING HRS FUNCTIONAL TEST SYSTEM USER");
            createIdamUserIfNotExists(
                SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION,
                SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION_ROLES
            );

            LOGGER.info("CREATING REGULAR TEST USERS");

            createIdamUserIfNotExists(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, CASE_WORKER_HRS_SEARCHER_ROLE);
            createIdamUserIfNotExists(USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY, CASE_WORKER_ROLE);
            createIdamUserIfNotExists(USER_WITH_NONACCESS_ROLE__CITIZEN, CITIZEN_ROLE);

            LOGGER.info("IMPORTING CCD DEFINITION");

            try {
                extendedCcdHelper.importDefinitionFile();
            } catch (IOException e) {
                LOGGER.error("IMPORTING CCD DEFINITION failed", e);
            }

            createUsersBaseTestRunCount++;

        }
        LOGGER.info("AUTHENTICATING TEST USER FOR CCD CALLS");

    }

    private void createIdamUserIfNotExists(String email, List<String> roles) {
        /*

        if multiple PR branches are triggered, then it means the user token cache used by em-test-helper
        will become stale

        potential for conflict with hrs-api using the hrs.tester@hmcts.net system user
        probably these tests should not use that user, however many issues arose when
        trying to refactor this logic and there was not enough time to see it through.

        good to have set to true for local environments, when testing role changes etc

        TODO make recreateUsers an environment value so that it is true for local dev, and false for AAT
         */
        boolean recreateUsers = true;

        if (recreateUsers) {
            LOGGER.info("CREATING USER {} with roles {}", email, roles);
            idamHelper.createUser(email, roles);
        } else {
            try {
                String userId = idamHelper.getUserId(email);
                LOGGER.info("User {} already exists: id={}", email, userId);
            } catch (Exception e) {
                //if user does not exist
                LOGGER.info(
                    "Exception thrown, likely user does not exist so will create. Ignore the above Exception:{}",
                    e.getMessage()
                );
                LOGGER.info("CREATING USER {} with roles {}", email, roles);
                idamHelper.createUser(email, roles);
            }
        }

    }

    @Test
    public void testHealthWelcomeEndpoint() {

        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .when()
            .get("/")
            .then()
            .statusCode(200)
            .body("message",equalTo(MESSAGE));
    }

    @Test
    public void testHealthEndpoint() {

        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
