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
import uk.gov.hmcts.reform.em.hrs.testutil.UploadToCVPBlobstore;
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
public class GetFileNameScenarios {

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Autowired
    AuthTokenGenerator authTokenGenerator;

    @Autowired
    private IdamHelper idamHelper;

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testGetFileNames() throws Exception {  // This test works locally if you comment out lines 48 and 49 and sett up the req body with a file in your local blobstore
        UploadToCVPBlobstore upload = new UploadToCVPBlobstore();
        upload.uploadBlob(); // this code works globally but the folder is being created with an empty name which the code is then struggling to pick up - once folder name is set it should work well

        JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
            "/functional-tests-1",
            "http://localhost:10000/devstoreaccount1/cvptestcontainer/functional-tests-1/quickstart.txt",
            "functional-tests-1/quickstart.txt",
            "txt",
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

        Thread.sleep(50000);

        ValidatableResponse response = RestAssured
            .given()
            .header("Authorization", idamHelper.authenticateUser("a@b.com"))
            .header("ServiceAuthorization", authTokenGenerator.generate())
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .get("/functional-tests-1")
            .then()
            .statusCode(200).log().all();

        System.out.println(response.extract().body().asString());
        assertEquals("functional-tests-1",response.extract().body().jsonPath().get("folder-name"));
        List<String> fileNames = response.extract().body().jsonPath().get("filenames");
        assertTrue(fileNames.stream().anyMatch(s -> s.equals("functional-tests-1/quickstart.txt")));
    }
}
