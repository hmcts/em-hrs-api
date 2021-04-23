package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.testutil.AuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {EmTestConfig.class, CcdAuthTokenGeneratorConfiguration.class, ExtendedCcdHelper.class, AuthTokenGeneratorConfiguration.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CreateHearingRecordingScenarios {

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Autowired
    AuthTokenGenerator authTokenGenerator;

    @Autowired
    private IdamHelper idamHelper;

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testCreate1HearingRecording() throws Exception {

        UUID id = UUID.randomUUID();

        JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
            "functional-tests-create-HR",
            "http://localhost:10000/devstoreaccount1/functional-tests/hearing-recording-segment/functional_test_HR1.m4a",
            "functional-tests/hearing-recording-segment-HR1"+id.toString(),
            "ma4",
            226200L,
            0
        );

        RestAssured
            .given()
            .header("Authorization", idamHelper.authenticateUser("a@b.com"))
            .header("ServiceAuthorization", authTokenGenerator.generate())
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when()
            .post("/segments")
            .then()
            .statusCode(202).log().all();

        Thread.sleep(3000);

    }

    @Test
    public void testCreate1000HearingRecordings() throws Exception {

        UUID id = UUID.randomUUID();
        int n = 1001;

        for (int i = 0; i < n; i++) {
            System.out.println(i);

            JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
                "functional-tests-create-HR",
                "http://localhost:10000/devstoreaccount1/functional-tests/hearing-recording-segment/functional_test_HR" + n + ".m4a",
                "functional-tests/hearing-recording-segment-HR" + n + id.toString(),
                "ma4",
                226200L,
                0
            );

            RestAssured
                .given()
                .header("Authorization", idamHelper.authenticateUser("a@b.com"))
                .header("ServiceAuthorization", authTokenGenerator.generate())
                .relaxedHTTPSValidation()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .body(reqBody)
                .when()
                .post("/segments")
                .then()
                .statusCode(202).log().all();

            Thread.sleep(3000);
        }
    }

    @Test
    public void testCreate1010HearingRecordings() throws Exception {

        UUID id = UUID.randomUUID();
        int n = 1009;

        for (int i = 0; i < n; i++) {
            System.out.println(i);

            JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
                "functional-tests-create-HR",
                "http://localhost:10000/devstoreaccount1/functional-tests/hearing-recording-segment/functional_test_HR" + n + ".m4a",
                "functional-tests/hearing-recording-segment-HR" + n + id.toString(),
                "ma4",
                226200L,
                0
            );

            RestAssured
                .given()
                .header("Authorization", idamHelper.authenticateUser("a@b.com"))
                .header("ServiceAuthorization", authTokenGenerator.generate())
                .relaxedHTTPSValidation()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .body(reqBody)
                .when()
                .post("/segments")
                .then()
                .statusCode(202).log().all();

        }

        n = 10;

        for (int i = 0; i < n; i++) {
            System.out.println(i);
            JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
                "functional-tests-create-HR",
                "http://localhost:10000/devstoreaccount1/functional-tests/hearing-recording-segment/functional_test_HR" + n + ".m4a",
                "functional-tests/hearing-recording-segment-HR" + n + id.toString(),
                "ma4",
                226200L,
                0
            );

            RestAssured
                .given()
                .header("Authorization", idamHelper.authenticateUser("a@b.com"))
                .header("ServiceAuthorization", authTokenGenerator.generate())
                .relaxedHTTPSValidation()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .body(reqBody)
                .when()
                .post("/segments")
                .then()
                .statusCode(202).log().all();

        }
    }
}
