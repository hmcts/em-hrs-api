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

import java.util.List;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HearingRecordingDeleteConsumerPactTest extends BaseConsumerPactTest {

    private static final String PROVIDER = "em_hrs_api_recording_delete_provider";
    private static final String CONSUMER = "em_hrs_api_recording_delete_consumer";
    private static final String DELETE_API_PATH = "/delete";

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact deleteHearingRecordings204(PactDslWithProvider builder) {
        DslPart requestBody = LambdaDsl.newJsonArrayMinLike(2, array ->
            array.numberType(162342324234L).numberType(3423432322333L)
        ).build();


        return builder
            .given("Hearing recordings exist for given CCD case IDs to delete")
            .uponReceiving("A valid delete request for hearing recordings")
            .path(DELETE_API_PATH)
            .method(HttpMethod.DELETE.toString())
            .headers(HEADERS_WITH_JSON)
            .body(requestBody)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "deleteHearingRecordings204", providerName = PROVIDER)
    void testDeleteHearingRecordings204(MockServer mockServer) {
        List<Long> ccdCaseIds = List.of(162342324234L, 3423432322333L);

        SerenityRest
            .given()
            .headers(HEADERS_WITH_JSON)
            .contentType(ContentType.JSON)
            .body(ccdCaseIds)
            .when()
            .delete(mockServer.getUrl() + DELETE_API_PATH)
            .then().log().all();
    }
}
