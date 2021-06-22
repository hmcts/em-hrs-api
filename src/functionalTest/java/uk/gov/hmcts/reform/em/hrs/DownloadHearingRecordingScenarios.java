package uk.gov.hmcts.reform.em.hrs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;


public class DownloadHearingRecordingScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;
    private String caseRef;
    private String filename;
    private CaseDetails caseDetails;
    private int expectedFileSize;

    @Before
    public void setup() throws Exception {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = randomCaseRef();
        filename = filename(caseRef);
        testUtil.uploadToCvpContainer(filename);

        postRecordingSegment(caseRef).then().statusCode(202);

        int count = 0;
        while (testUtil.checkIfUploadedToCvp(FOLDER) <= 0) {
            TimeUnit.SECONDS.sleep(30);
            count++;

            if (count > 10) {
                break;
            }
        }

        caseDetails = findCase(caseRef);


        expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));
    }

    @After
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    public void userWithCaseWorkerHrsRoleShouldBeAbleToDownloadHearingRecordings() {
        final byte[] downloadedFileBytes =
            downloadRecording(CASEWORKER_HRS_USER, CASE_WORKER_HRS_ROLE, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void userWithCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(CASEWORKER_USER, CASE_WORKER_ROLE, caseDetails.getData())
            .then()
            .statusCode(403);
    }

    @Test
    public void userWithCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(CITIZEN_USER, CITIZEN_ROLE, caseDetails.getData())
            .then()
            .statusCode(403);
    }
}
