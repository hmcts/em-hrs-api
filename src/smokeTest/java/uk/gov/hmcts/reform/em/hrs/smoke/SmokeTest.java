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

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@SpringBootTest(classes = {
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

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    public SmokeTest() {

    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        SerenityRest.useRelaxedHTTPSValidation();
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
