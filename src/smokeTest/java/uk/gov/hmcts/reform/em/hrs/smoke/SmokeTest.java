package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.yml")
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to DM Store API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthWelcomeEndpoint() {
        SerenityRest.useRelaxedHTTPSValidation();

        Map responseMap =
            SerenityRest
                .given()
                .baseUri(testUrl)
                .get("/")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Map.class);

        Assert.assertEquals(1, responseMap.size());
        Assert.assertEquals(MESSAGE, responseMap.get("message"));
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
