package uk.gov.hmcts.reform.em.hrs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

public class HearingRecordingSegmentScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;
    String caseRef;
    String filename;

    @Before
    public void setup() throws Exception {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = randomCaseRef();
        filename = filename(caseRef);
        testUtil.uploadToCvpContainer(filename);

        int counter = 0;
        while (testUtil.checkIfUploadedToCVP(FOLDER) <= 0) {
            TimeUnit.SECONDS.sleep(30);
            counter++;

            if (counter > 10){
                throw new IllegalStateException("could not find files");
            }
        }
    }

    @After
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    public void shouldCreateHearingRecordingSegment() throws Exception {
        postRecordingSegment(caseRef)
            .then()
            .log().all()
            .statusCode(202);

        int count = 0;
        while (testUtil.checkIfUploadedToHRS(FOLDER) <= 0) {
            TimeUnit.SECONDS.sleep(30);
            count++;

            if (count > 10) {
                throw new IllegalStateException("could not find files within test");
            }
        }

        getFilenames(FOLDER)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(FOLDER))
            .body("filenames", hasSize(1))
            .body("filenames", contains(filename));
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
