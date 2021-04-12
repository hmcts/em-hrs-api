package uk.gov.hmcts.reform.em.hrs;

import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {EmTestConfig.class, CcdAuthTokenGeneratorConfiguration.class, ExtendedCcdHelper.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseUpdateScenarios {

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Rule
    public RetryRule retryRule = new RetryRule(1);

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testCcdCaseUpdate() {
        HearingRecordingDto reqBody = extendedCcdHelper.createRecordingSegment(
            "http://dm-store:8080/documents/e486435e-30e8-456c-9d4d-4adffcb50010",
            "hearing-recording-segment",
            ".mp4",
            12L,
            0
            );

        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when()
            .post("/segments")
            .then()
            .statusCode(202);
    }
}
