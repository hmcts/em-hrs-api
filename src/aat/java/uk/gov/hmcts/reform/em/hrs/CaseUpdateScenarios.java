package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {EmTestConfig.class, CcdAuthTokenGeneratorConfiguration.class, ExtendedCcdHelper.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseUpdateScenarios {

    private static final String JURISDICTION = "HRS";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final String SHARE_FILES = "shareFiles";
    public static final String hrsEmail = "sharee.tester@test.com";

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Rule
    public RetryRule retryRule = new RetryRule(1);

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testCcdCaseUpdate() {

        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .get("/folders/audiostream01")
            .then()
            .statusCode(200);

        JsonNode reqBody = extendedCcdHelper.createRecordingSegment(
            "audiostream01",
            "http://localhost:10000/devstoreaccount1/cvptestcontainer/audiostream01/audio_test.m4a",
            "audiostream01/audio_test.m4a",
            "ma4",
            226200L,
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

    @Test
    public void testDocumentShare() {
        Map<String, String> tokens = extendedCcdHelper.getTokens();
        Long caseId = 1618821731433778L;
        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(tokens.get("user"), tokens.get("service"),
                                                                           caseId.toString(), SHARE_FILES
        );

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(extendedCcdHelper.getShareRequest(hrsEmail)).build();

        coreCaseDataApi
            .submitEventForCaseWorker(tokens.get("user"), tokens.get("service"), tokens.get("userId"),
                                      JURISDICTION, CASE_TYPE, caseId.toString(), false, caseData);
    }


}

