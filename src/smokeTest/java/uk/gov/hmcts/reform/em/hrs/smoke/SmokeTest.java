package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

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
@TestInstance(PER_CLASS)
public class SmokeTest {
    private static final String MESSAGE = "Welcome to the HRS API!";

    private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTest.class);

    public static final String SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION =
        "hrs.functional.system.user@hmcts.net";

    private static final String ROLE_CASE_WORKER = "caseworker";
    public static final List<String>
        SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION_ROLES =
        List.of(
            ROLE_CASE_WORKER,
            "caseworker-hrs",
            "caseworker-hrs-searcher",
            "ccd-import",
            "caseworker-hrs-systemupdate"
        );

    protected static final String USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS = "em-test-searcher@test.hmcts.net";
    protected static final String USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY = "em-test-requestor@test.hmcts.net";
    protected static final String USER_WITH_NONACCESS_ROLE_CITIZEN = "em-test-citizen@test.hmcts.net";
    protected static final List<String> CASE_WORKER_ROLE = List.of(ROLE_CASE_WORKER);
    protected static final List<String> CASE_WORKER_HRS_SEARCHER_ROLE =
        List.of(ROLE_CASE_WORKER, "caseworker-hrs", "caseworker-hrs-searcher");
    protected static final List<String> CITIZEN_ROLE = List.of("citizen");

    @Value("${test.url}")
    private String testUrl;

    @Value("${upload-ccd-definition}")
    protected boolean uploadCcdDefinition;
    protected ExtendedCcdHelper extendedCcdHelper;
    protected IdamHelper idamHelper;

    @Autowired
    public SmokeTest(ExtendedCcdHelper extendedCcdHelper, IdamHelper idamHelper) {
        this.extendedCcdHelper = extendedCcdHelper;
        this.idamHelper = idamHelper;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {

        LOGGER.info("INITIALISING SMOKE TESTS, upload Ccd Definition: {} ", uploadCcdDefinition);

        LOGGER.info("BASE TEST POST CONSTRUCT INITIALISATIONS....");
        SerenityRest.useRelaxedHTTPSValidation();


        LOGGER.info("CREATING HRS FUNCTIONAL TEST SYSTEM USER");
        createIdamUser(
            SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION,
            SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION_ROLES
        );

        LOGGER.info("CREATING REGULAR TEST USERS");

        createIdamUser(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, CASE_WORKER_HRS_SEARCHER_ROLE);
        createIdamUser(USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY, CASE_WORKER_ROLE);
        createIdamUser(USER_WITH_NONACCESS_ROLE_CITIZEN, CITIZEN_ROLE);

        LOGGER.info("IMPORTING CCD DEFINITION");
        extendedCcdHelper.importDefinitionFile();

    }

    private void createIdamUser(String email, List<String> roles) {
        LOGGER.info("CREATING USER {} with roles {}", email, roles);
        idamHelper.createUser(email, roles);
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
