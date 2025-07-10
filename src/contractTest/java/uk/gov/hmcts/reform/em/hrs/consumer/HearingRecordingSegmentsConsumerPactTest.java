package uk.gov.hmcts.reform.em.hrs.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HearingRecordingSegmentsConsumerPactTest extends BaseConsumerPactTest {

    private static final String PROVIDER = "em_hrs_api_recording_segments_provider";
    private static final String CONSUMER = "em_hrs_api_recording_segments_consumer";
    private static final String SEGMENT_API_PATH_TEMPLATE = "/hearing-recordings/%s/segments/%d";
    private static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    private static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";

    private static final UUID RECORDING_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final int SEGMENT_NO = 1;
    private static final String FILE_NAME = "testfile.mp3";
    private static final String FOLDER_NAME = "folderA";

    public Map<String, String> getHeaders() {
        return Map.of(
            "Authorization", AUTH_TOKEN,
            "ServiceAuthorization", SERVICE_AUTH_TOKEN
        );
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact getFileByFileName(PactDslWithProvider builder) {
        byte[] expectedBody = new byte[1024];
        Arrays.fill(expectedBody, (byte) 'f');
        return builder
            .given("A hearing recording file exists for download by file name")
            .uponReceiving("A GET request for a hearing recording file by file name")
            .path("/hearing-recordings/" + RECORDING_ID + "/file/" + FILE_NAME)
            .method("GET")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(
                "Content-Type", "text/plain",
                "Content-Disposition", "attachment; filename=folderA/testfile.mp3",
                "Accept-Ranges", "bytes",
                "Content-Length", "1024"
            ))
            .withBinaryData(expectedBody, "text/plain")
            .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact getFileByFolderAndFileName(PactDslWithProvider builder) {
        byte[] expectedBody = new byte[1024];
        Arrays.fill(expectedBody, (byte) 'f');
        return builder
            .given("A hearing recording file exists for download by folder and file name")
            .uponReceiving("A GET request for a hearing recording file by folder and file name")
            .path("/hearing-recordings/" + RECORDING_ID + "/file/" + FOLDER_NAME + "/" + FILE_NAME)
            .method("GET")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(
                "Content-Type", "text/plain",
                "Content-Disposition", "attachment; filename=folderA/testfile.mp3",
                "Accept-Ranges", "bytes",
                "Content-Length", "1024"
            ))
            .withBinaryData(expectedBody, "text/plain")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getFileByFileName", providerName = PROVIDER)
    void testGetFileByFileName(MockServer mockServer) {
        Response response = io.restassured.RestAssured
            .given()
            .headers(getHeaders())
            .get(mockServer.getUrl() + "/hearing-recordings/" + RECORDING_ID + "/file/" + FILE_NAME);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.header("Content-Type")).isEqualTo("text/plain");
        assertThat(response.asByteArray()).hasSize(1024);
    }

    @Test
    @PactTestFor(pactMethod = "getFileByFolderAndFileName", providerName = PROVIDER)
    void testGetFileByFolderAndFileName(MockServer mockServer) {
        Response response = io.restassured.RestAssured
            .given()
            .headers(getHeaders())
            .get(mockServer.getUrl() + "/hearing-recordings/"
                     + RECORDING_ID + "/file/"
                     + FOLDER_NAME + "/" + FILE_NAME);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.header("Content-Type")).isEqualTo("text/plain");
        assertThat(response.asByteArray()).hasSize(1024);
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegment200(PactDslWithProvider builder) {
        String path = String.format(SEGMENT_API_PATH_TEMPLATE, RECORDING_ID, SEGMENT_NO);
        byte[] expectedBody = new byte[1024];
        Arrays.fill(expectedBody, (byte) 'i'); // Fill with ASCII 'i'
        return builder
            .given("A hearing recording segment exists for download")
            .uponReceiving("A GET request for a hearing recording segment")
            .path(path)
            .method("GET")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(
                "Content-Type", "text/plain",
                "Content-Disposition", "attachment; filename=testfile.mp3",
                "Accept-Ranges", "bytes",
                "Content-Length", "1024"
            ))
            .withBinaryData(expectedBody, "text/plain")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegment200", providerName = PROVIDER)
    void testDownloadSegment200(MockServer mockServer) {
        String path = String.format(SEGMENT_API_PATH_TEMPLATE, RECORDING_ID, SEGMENT_NO);

        Response response = SerenityRest
            .given()
            .headers(getHeaders())
            .get(mockServer.getUrl() + path)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract().response();

        // Assert headers
        assertThat(response.getHeader("Content-Type")).isEqualTo("text/plain");
        assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=testfile.mp3");
        assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes");

    }
}
