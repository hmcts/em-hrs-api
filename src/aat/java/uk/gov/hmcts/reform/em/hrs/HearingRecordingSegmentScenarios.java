package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;


import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;


public class HearingRecordingSegmentScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;


    @Before
    public void setup() throws Exception {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
        fileName = String.format(fileName, counter.incrementAndGet());
        testUtil.uploadToCvpContainer(fileName);
        createFolderIfDoesNotExistInHrsDB(FOLDER);
    }

    @After
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    public void shouldCreateHearingRecordingSegment() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(30);

        final ValidatableResponse validatableResponse = getFilenames(FOLDER);

        validatableResponse
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(FOLDER))
            .body("filenames", hasSize(1))
            .body("filenames[0]", equalTo(fileName));
    }

    @Test
    public void shouldCreateFolderWhenDoesNotExistAndReturnEmptyFileNames() {
        final String nonExistentFolder = "audiostream000000";

        getFilenames(nonExistentFolder)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(nonExistentFolder))
            .body("filenames", empty());
    }


}
