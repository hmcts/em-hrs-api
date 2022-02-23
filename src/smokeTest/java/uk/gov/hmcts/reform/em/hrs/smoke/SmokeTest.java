package uk.gov.hmcts.reform.em.hrs.smoke;

import io.restassured.RestAssured;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.yml")
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {

        String response =
            RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(testUrl)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get("$.status");
        assertEquals("UP", response);

    }

}
