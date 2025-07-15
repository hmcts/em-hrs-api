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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HearingRecordingDownloadShareeConsumerPactTest extends BaseConsumerPactTest {

    private static final String PROVIDER = "em_hrs_api_recording_download_sharee_provider";
    private static final String CONSUMER = "em_hrs_api_recording_download_sharee_consumer";

    private static final UUID RECORDING_ID = UUID.fromString("3d0a6e15-1f16-4c9b-b087-52d84de469d0");
    private static final int SEGMENT_NUMBER = 1;
    private static final String FILE_NAME = "mocked-file.mp3";
    private static final String FOLDER_NAME = "folderA";

    private static final String SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO = "/hearing-recordings/"
        + RECORDING_ID + "/segments/" + SEGMENT_NUMBER + "/sharee";

    private static final String SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME = "/hearing-recordings/"
        + RECORDING_ID + "/file/" + FILE_NAME + "/sharee";

    private static final String SEGMENT_DOWNLOAD_PATH_BY_FOLDER_AND_FILE_NAME = "/hearing-recordings/"
        + RECORDING_ID + "/file/" + FOLDER_NAME + FILE_NAME + "/sharee";

    private static byte[] CONTENT = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10, 0x20, 0x30, 0x40};

    private static Map<String, String> headers = Map.of(
        HttpHeaders.AUTHORIZATION, "Bearer some-user-token",
        "serviceauthorization", "Bearer some-service-token"
    );

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegmentBySegmentNoPact(PactDslWithProvider builder) {
        return builder
            .given("A segment exists for sharee to download by recording ID and segment number")
            .uponReceiving("A request to download a segment for a hearing recording")
            .path(SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO)
            .method("GET")
            .headers(headers)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(
                Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                       HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mocked-file.mp3")
            )
            .withBinaryData(CONTENT, "application/octet-stream")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegmentBySegmentNoPact", providerName = PROVIDER)
    void testDownloadSegmentBySegmentNo(MockServer mockServer) {
        Response response = SerenityRest
            .given()
            .headers(headers)
            .get(mockServer.getUrl() + SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO);

        response.then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        assertThat(response.asByteArray()).hasSize(8);
        assertThat(response.getHeader("Content-Disposition"))
            .isEqualTo("attachment; filename=mocked-file.mp3");
    }


    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegmentByFileNameNoPact(PactDslWithProvider builder) {
        return builder
            .given("A segment exists for sharee to download by recording ID and file name")
            .uponReceiving("A request to download a segment for a hearing recording by file name")
            .path(SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME)
            .method("GET")
            .headers(headers)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(
                Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                       HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mocked-file.mp3")
            )
            .withBinaryData(CONTENT, "application/octet-stream")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegmentByFileNameNoPact", providerName = PROVIDER)
    void testDownloadSegmentByFileNameNo(MockServer mockServer) {
        Response response = SerenityRest
            .given()
            .headers(headers)
            .get(mockServer.getUrl() + SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME);

        response.then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        assertThat(response.asByteArray()).hasSize(8);
        assertThat(response.getHeader("Content-Disposition"))
            .isEqualTo("attachment; filename=mocked-file.mp3");
    }

}
