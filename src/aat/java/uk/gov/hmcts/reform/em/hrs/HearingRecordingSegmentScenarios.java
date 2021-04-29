package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

public class HearingRecordingSegmentScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;

    @BeforeEach
    public void setup() throws Exception {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);

        final String id = UUID.randomUUID().toString();
        fileName = String.format(fileName, id);
        testUtil.uploadToCvpContainer(fileName);
        createFolderIfDoesNotExistInHrsDB(FOLDER);
    }

    @AfterEach
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    @DisplayName("Create a segment and verify it has been created")
    public void shouldCreateHearingRecordingSegment() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final ValidatableResponse validatableResponse = getRecordingFileNames(FOLDER);

        validatableResponse
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(FOLDER))
            .body("filenames", hasSize(1))
            .body("filenames[0]", equalTo(fileName));
    }

    @Test
    @DisplayName("Should create a folder when it does not exist and return empty filenames")
    public void shouldCreateFolderWhenDoesNotExistAndReturnEmptyFileNames() {
        final String nonExistentFolder = "audiostream000000";

        getRecordingFileNames(nonExistentFolder)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(nonExistentFolder))
            .body("filenames", empty());
    }
}
