package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.smoke.config.SmokeTestContextConfiguration;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = SmokeTestContextConfiguration.class)
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Welcome to the HRS API!";

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
