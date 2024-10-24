package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Ignore;
import org.junit.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Ignore
public class SecurityScenarios extends BaseTest {

    @Test
    public void getFolderShouldReturn401WhenS2STokenNotValid() {
        SerenityRest
            .given()
            .header("ServiceAuthorization", "BEARER notValidTokenTest")
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .log()
            .all()
            .get("/folders/NON_EXISTING_FOLDER")
            .then()
            .statusCode(401);
    }


    @Test
    public void getFolderShouldReturn401WhenS2sMissingAuthTokenValid() {
        var userToken = idamHelper.authenticateUser(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);
        SerenityRest
            .given()
            .header("Authorization", userToken)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .log()
            .all()
            .get("/folders/NON_EXISTING_FOLDER")
            .then()
            .statusCode(401);
    }

    @Test
    public void getFolderShouldReturn200WhenS2STokenIsValid() {


        this.authRequestForHrsIngestor()
            .when()
            .log()
            .all()
            .get("/folders/NON_EXISTING_FOLDER")
            .then()
            .statusCode(200);
    }

    @Test
    public void postRecordingSegmentShouldReturn401WhenS2STokenIsValid() {
        final JsonNode segmentPayload = createSegmentPayload("caseRef", 0);
        var userToken = idamHelper.authenticateUser(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);

        SerenityRest
            .given()
            .header("ServiceAuthorization", "BEARER notValidTokenTest")
            .header("Authorization", userToken)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(segmentPayload)
            .when()
            .log()
            .all()
            .post("/segments")
            .then()
            .log()
            .all()
            .statusCode(401);
    }
}
