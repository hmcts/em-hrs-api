package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SecurityScenarios extends BaseTest {


    public static final String NON_EXISTING_FOLDER_PATH = "/folders/NON_EXISTING_FOLDER";

    @Autowired
    SecurityScenarios(
        IdamClient idamClient,
        IdamHelper idamHelper,
        S2sHelper s2sHelper,
        CoreCaseDataApi coreCaseDataApi,
        ExtendedCcdHelper extendedCcdHelper
    ) {
        super(idamClient, idamHelper, s2sHelper, coreCaseDataApi, extendedCcdHelper);
    }

    @Test
    public void getFolderShouldReturn401WhenS2STokenNotValid() {
        SerenityRest
            .given()
            .header("ServiceAuthorization", "BEARER notValidTokenTest")
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .get(NON_EXISTING_FOLDER_PATH)
            .then()
            .statusCode(401);
    }


    @Test
    public void getFolderShouldReturn401WhenS2sMissingAuthTokenValid() {
        var userToken = idamHelper.authenticateUser(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS);
        SerenityRest
            .given()
            .header("Authorization", userToken)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .get(NON_EXISTING_FOLDER_PATH)
            .then()
            .statusCode(401);
    }

    @Test
    public void getFolderShouldReturn200WhenS2STokenIsValid() {


        this.authRequestForHrsIngestor()
            .when()
            .get(NON_EXISTING_FOLDER_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    public void postRecordingSegmentShouldReturn401WhenS2STokenIsValid() {
        final JsonNode segmentPayload = createSegmentPayload("caseRef", 0);
        var userToken = idamHelper.authenticateUser(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS);

        SerenityRest
            .given()
            .header("ServiceAuthorization", "BEARER notValidTokenTest")
            .header("Authorization", userToken)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(segmentPayload)
            .when()
            .post("/segments")
            .then()
            .statusCode(401);
    }
}
