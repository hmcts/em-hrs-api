package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.yml")
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to the HRS API!";

    @Value("${test.url}")
    private String testUrl;

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
