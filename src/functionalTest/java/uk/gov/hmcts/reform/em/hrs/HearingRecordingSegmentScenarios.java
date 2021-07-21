package uk.gov.hmcts.reform.em.hrs;


import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

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
        int cvpBlobCount = testUtil.getCvpBlobCount(FOLDER);
        testUtil.uploadToCvpContainer(filename);
        testUtil.checkIfUploadedToCvp(FOLDER, cvpBlobCount);
    }

    @Test
    public void shouldCreateHearingRecordingSegment() throws Exception {
        int hrsBlobCount = testUtil.getHrsBlobCount(FOLDER);

        postRecordingSegment(caseRef)
            .then()
            .log().all()
            .statusCode(202);

        testUtil.checkIfUploadedToHrs(FOLDER, hrsBlobCount);
        getFilenames(FOLDER)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(FOLDER))
            .body("filenames", hasItem(filename));
    }

    @Test
    public void shouldNotCopyHearingRecordingSegmentWhenFileNameMalformed() throws Exception {
        caseRef = "I'm malformed now " + caseRef + " I'm malformed now";
        postRecordingSegment(caseRef)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(30);

        getFilenames(FOLDER)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(FOLDER))
            .body("filenames", not(hasItem(filename)));
    }

    @Test
    public void shouldCreateFolderWhenDoesNotExistAndReturnEmptyFileNames() {
        final String nonExistentFolder = "dummyaudiostream000000";

        getFilenames(nonExistentFolder)
            .assertThat().log().all()
            .statusCode(200)
            .body("folder-name", equalTo(nonExistentFolder))
            .body("filenames", empty());
    }
}
