package uk.gov.hmcts.reform.em.hrs;


import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
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

import javax.inject.Inject;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {EmTestConfig.class, CcdAuthTokenGeneratorConfiguration.class, ExtendedCcdHelper.class, AuthTokenGeneratorConfiguration.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
public class DownloadScenarios { // this needs to do soft coded suing the cvp blobstore - make sure segmesnt and recording id match the blobstore file

    @Autowired
    AuthTokenGenerator authTokenGenerator;

    @Autowired
    private IdamHelper idamHelper;

    @Value("${test.url}")
    private String testUrl;

    String Recordingid = "b656977b-a7b0-4599-a845-14bf5ab9bf6a"; // this also works locally but needs to be set up to work through jenkins using the blobstore
    private int segmentNumber = 0;

    @Test
    public void testGetHearingRecording() {
        RestAssured
            .given()
            .header("Authorization", idamHelper.authenticateUser("a@b.com"))
            .header("ServiceAuthorization", authTokenGenerator.generate())
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .get("/hearing-recordings/" + Recordingid + "/segments/" + segmentNumber)
            .then()
            .statusCode(200).log().all();
    }
}
