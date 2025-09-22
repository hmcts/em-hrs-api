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
    public static final String SHAREE = "/sharee";
    public static final String HEARING_RECORDINGS = "/hearing-recordings/";
    private static final String SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO = HEARING_RECORDINGS
        + RECORDING_ID + "/segments/" + SEGMENT_NUMBER + SHAREE;

    private static final String SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME = HEARING_RECORDINGS
        + RECORDING_ID + "/file/" + FILE_NAME + SHAREE;

    private static final String SEGMENT_DOWNLOAD_PATH_BY_FOLDER_AND_FILE_NAME = HEARING_RECORDINGS
        + RECORDING_ID + "/file/" + FOLDER_NAME + FILE_NAME + SHAREE;

    private void testDownload(MockServer mockServer, String reqUrl) {
        Response response = SerenityRest
            .given()
            .headers(HEADERS_WITHOUT_JSON)
            .get(mockServer.getUrl() + reqUrl);

        response.then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        assertThat(response.asByteArray()).hasSize(8);
        assertThat(response.getHeader("Content-Disposition"))
            .isEqualTo("attachment; filename=mocked-file.mp3");
    }

    public V4Pact buildPact(PactDslWithProvider builder, String given, String path) {
        return builder
            .given(given)
            .uponReceiving("A request to download a segment for a hearing recording")
            .path(path)
            .method("GET")
            .headers(HEADERS_WITHOUT_JSON)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(
                Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                       HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mocked-file.mp3"
                )
            )
            .withBinaryData(DOWNLOAD_CONTENT, "application/octet-stream")
            .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegmentBySegmentNoPact(PactDslWithProvider builder) {
        return buildPact(
            builder,
            "A segment exists for sharee to download by recording ID and segment number",
            SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO
        );
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegmentBySegmentNoPact", providerName = PROVIDER)
    void testDownloadSegmentBySegmentNo(MockServer mockServer) {
        testDownload(mockServer, SEGMENT_DOWNLOAD_PATH_BY_SEGMENT_NO);
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegmentByFileNameNoPact(PactDslWithProvider builder) {
        return buildPact(
            builder,
            "A segment exists for sharee to download by recording ID and file name",
            SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME
        );
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegmentByFileNameNoPact", providerName = PROVIDER)
    void testDownloadSegmentByFileNameNo(MockServer mockServer) {
        testDownload(mockServer, SEGMENT_DOWNLOAD_PATH_BY_FILE_NAME);
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public V4Pact downloadSegmentByFolderAndFileNameNoPact(PactDslWithProvider builder) {
        return buildPact(
            builder,
            "A segment exists for sharee to download by recording ID and folder and file name",
            SEGMENT_DOWNLOAD_PATH_BY_FOLDER_AND_FILE_NAME
        );
    }

    @Test
    @PactTestFor(pactMethod = "downloadSegmentByFolderAndFileNameNoPact", providerName = PROVIDER)
    void testDownloadSegmentByFolderAndFileNameNo(MockServer mockServer) {
        testDownload(mockServer, SEGMENT_DOWNLOAD_PATH_BY_FOLDER_AND_FILE_NAME);
    }
}
