package uk.gov.hmcts.reform.em.hrs.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HearingRecordingShareesConsumerPactTest extends BaseConsumerPactTest {

    private static final String PROVIDER = "em_hrs_api_recording_sharees_provider";
    private static final String CONSUMER = "em_hrs_api_recording_sharees_consumer";
    private static final String SHAREES_API_PATH = "/sharees";

    // Test data constants
    private static final long CASE_ID = 1234567890123456L;
    private static final String JURISDICTION = "SAMPLE_JURISDICTION";
    private static final String CASE_TYPE = "SAMPLE_CASE_TYPE";
    private static final String CREATED_DATE = "2024-07-04T07:50:00";
    private static final String LAST_MODIFIED = "2024-07-04T08:00:00";
    private static final String STATE = "Open";
    private static final int LOCKED_BY_USER_ID = 12345;
    private static final int SECURITY_LEVEL = 1;
    private static final String SECURITY_CLASSIFICATION = "PUBLIC";
    private static final String CALLBACK_RESPONSE_STATUS = "SUCCESS";
    private static final String EVENT_ID = "share_event";

    /**
     * Defines the Pact for a successful POST to /sharees.
     */
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact createSharees200(PactDslWithProvider builder) {
        return builder
            .given("Permission record can be created for a hearing recording and user notified")
            .uponReceiving("A valid POST request to create a sharees record")
            .path(SHAREES_API_PATH)
            .method(HttpMethod.POST.toString())
            .headers(getHeaders())
            .body(buildShareesRequestBody())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .toPact(V4Pact.class);
    }

    /**
     * Tests the POST /sharees contract.
     */
    @Test
    @PactTestFor(pactMethod = "createSharees200", providerName = PROVIDER)
    void testCreateSharees200(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHeaders())
            .contentType(ContentType.JSON)
            .body(buildShareesRequestBody().getBody().toString())
            .post(mockServer.getUrl() + SHAREES_API_PATH)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private DslPart buildShareesRequestBody() {
        return LambdaDsl.newJsonBody(body -> {
            body.object("case_details", caseDetails -> {
                caseDetails.numberType("id", CASE_ID);
                caseDetails.stringType("jurisdiction", JURISDICTION);
                caseDetails.stringType("case_type_id", CASE_TYPE);
                caseDetails.stringType("created_date", CREATED_DATE);
                caseDetails.stringType("last_modified", LAST_MODIFIED);
                caseDetails.stringType("state", STATE);
                caseDetails.integerType("locked_by_user_id", LOCKED_BY_USER_ID);
                caseDetails.integerType("security_level", SECURITY_LEVEL);
                caseDetails.object("case_data", data -> data.stringType("someField", "someValue"));
                caseDetails.stringType("security_classification", SECURITY_CLASSIFICATION);
                caseDetails.stringType("callback_response_status", CALLBACK_RESPONSE_STATUS);
            });
            body.object("case_details_before", caseDetailsBefore -> {
                caseDetailsBefore.numberType("id", CASE_ID);
                caseDetailsBefore.stringType("jurisdiction", JURISDICTION);
                caseDetailsBefore.stringType("case_type_id", CASE_TYPE);
                caseDetailsBefore.stringType("created_date", "2024-07-04T07:40:00");
                caseDetailsBefore.stringType("last_modified", CREATED_DATE);
                caseDetailsBefore.stringType("state", STATE);
                caseDetailsBefore.integerType("locked_by_user_id", LOCKED_BY_USER_ID);
                caseDetailsBefore.integerType("security_level", SECURITY_LEVEL);
                caseDetailsBefore.object("case_data", data -> data.stringType("someField", "someOldValue"));
                caseDetailsBefore.stringType("security_classification", SECURITY_CLASSIFICATION);
                caseDetailsBefore.stringType("callback_response_status", CALLBACK_RESPONSE_STATUS);
            });
            body.stringType("event_id", EVENT_ID);
        }).build();
    }
}
