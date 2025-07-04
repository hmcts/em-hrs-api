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

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact createSharees200(PactDslWithProvider builder) {
        DslPart requestBody = LambdaDsl.newJsonBody((body) -> {
            body.object("case_details", (caseDetails) -> {
                caseDetails.numberType("id", 1234567890123456L);
                caseDetails.stringType("jurisdiction", "SAMPLE_JURISDICTION");
                caseDetails.stringType("case_type_id", "SAMPLE_CASE_TYPE");
                caseDetails.stringType("created_date", "2024-07-04T07:50:00");
                caseDetails.stringType("last_modified", "2024-07-04T08:00:00");
                caseDetails.stringType("state", "Open");
                caseDetails.integerType("locked_by_user_id", 12345);
                caseDetails.integerType("security_level", 1);
                caseDetails.object("case_data", (data) -> {
                    data.stringType("someField", "someValue");
                });
                caseDetails.stringType("security_classification", "PUBLIC");
                caseDetails.stringType("callback_response_status", "SUCCESS");
            });
            body.object("case_details_before", (caseDetailsBefore) -> {
                caseDetailsBefore.numberType("id", 1234567890123456L);
                caseDetailsBefore.stringType("jurisdiction", "SAMPLE_JURISDICTION");
                caseDetailsBefore.stringType("case_type_id", "SAMPLE_CASE_TYPE");
                caseDetailsBefore.stringType("created_date", "2024-07-04T07:40:00");
                caseDetailsBefore.stringType("last_modified", "2024-07-04T07:50:00");
                caseDetailsBefore.stringType("state", "Open");
                caseDetailsBefore.integerType("locked_by_user_id", 12345);
                caseDetailsBefore.integerType("security_level", 1);
                caseDetailsBefore.object("case_data", (data) -> {
                    data.stringType("someField", "someOldValue");
                });
                caseDetailsBefore.stringType("security_classification", "PUBLIC");
                caseDetailsBefore.stringType("callback_response_status", "SUCCESS");
            });
            body.stringType("event_id", "share_event");
        }).build();

        return builder
            .given("Permission record can be created for a hearing recording and user notified")
            .uponReceiving("A valid POST request to create a sharees record")
            .path(SHAREES_API_PATH)
            .method(HttpMethod.POST.toString())
            .headers(getHeaders())
            .body(requestBody)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createSharees200", providerName = PROVIDER)
    void testCreateSharees200(MockServer mockServer) {
        DslPart requestBody = LambdaDsl.newJsonBody((body) -> {
            body.object("case_details", (caseDetails) -> {
                caseDetails.numberType("id", 1234567890123456L);
                caseDetails.stringType("jurisdiction", "SAMPLE_JURISDICTION");
                caseDetails.stringType("case_type_id", "SAMPLE_CASE_TYPE");
                caseDetails.stringType("created_date", "2024-07-04T07:50:00");
                caseDetails.stringType("last_modified", "2024-07-04T08:00:00");
                caseDetails.stringType("state", "Open");
                caseDetails.integerType("locked_by_user_id", 12345);
                caseDetails.integerType("security_level", 1);
                caseDetails.object("case_data", (data) -> {
                    data.stringType("someField", "someValue");
                });
                caseDetails.stringType("security_classification", "PUBLIC");
                caseDetails.stringType("callback_response_status", "SUCCESS");
            });
            body.object("case_details_before", (caseDetailsBefore) -> {
                caseDetailsBefore.numberType("id", 1234567890123456L);
                caseDetailsBefore.stringType("jurisdiction", "SAMPLE_JURISDICTION");
                caseDetailsBefore.stringType("case_type_id", "SAMPLE_CASE_TYPE");
                caseDetailsBefore.stringType("created_date", "2024-07-04T07:40:00");
                caseDetailsBefore.stringType("last_modified", "2024-07-04T07:50:00");
                caseDetailsBefore.stringType("state", "Open");
                caseDetailsBefore.integerType("locked_by_user_id", 12345);
                caseDetailsBefore.integerType("security_level", 1);
                caseDetailsBefore.object("case_data", (data) -> {
                    data.stringType("someField", "someOldValue");
                });
                caseDetailsBefore.stringType("security_classification", "PUBLIC");
                caseDetailsBefore.stringType("callback_response_status", "SUCCESS");
            });
            body.stringType("event_id", "share_event");
        }).build();

        SerenityRest
            .given()
            .headers(getHeaders())
            .contentType(ContentType.JSON)
            .body(requestBody.getBody().toString())
            .post(mockServer.getUrl() + SHAREES_API_PATH)
            .then()
            .statusCode(HttpStatus.OK.value());
    }
}
