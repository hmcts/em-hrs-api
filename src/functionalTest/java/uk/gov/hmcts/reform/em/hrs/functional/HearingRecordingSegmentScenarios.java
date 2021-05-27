package uk.gov.hmcts.reform.em.hrs.functional;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.functional.util.TestUtil;

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
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        testUtil.uploadToCvpContainer(FOLDER + "/" + fileName);
    }

//    @After
//    public void clear() {
//        testUtil.deleteFileFromHrsContainer(FOLDER);
//        testUtil.deleteFileFromCvpContainer(FOLDER);
//    }

    @Test
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
            .body("folder-name", equalTo("test"))
            .body("filenames", hasSize(1))
            .body("filenames[0]", equalTo(fileName));

    }

//    @Test
//    public void shouldCreateFolderWhenDoesNotExistAndReturnEmptyFileNames() {
//        final String nonExistentFolder = "audiostream000000";
//
//        getRecordingFileNames(nonExistentFolder)
//            .assertThat().log().all()
//            .statusCode(200)
//            .body("folder-name", equalTo(nonExistentFolder))
//            .body("filenames", empty());
//    }
}
