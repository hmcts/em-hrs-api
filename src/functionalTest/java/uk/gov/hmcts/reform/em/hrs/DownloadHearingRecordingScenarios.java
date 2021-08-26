package uk.gov.hmcts.reform.em.hrs;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

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
        caseRef = timebasedCaseRef();
        filename = filename(caseRef);

        int cvpBlobCount = testUtil.getCvpBlobCount(FOLDER);
        testUtil.uploadToCvpContainer(filename);
        testUtil.checkIfUploadedToCvp(FOLDER, cvpBlobCount);

        int hrsBlobCount = testUtil.getHrsBlobCount(FOLDER);
        postRecordingSegment(caseRef).then().statusCode(202);
        testUtil.checkIfUploadedToHrs(FOLDER, hrsBlobCount);

        caseDetails = findCase(caseRef);

        expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));
    }

    @Test
    public void userWithCaseWorkerHrsRoleShouldBeAbleToDownloadHearingRecordings() {
        final byte[] downloadedFileBytes =
            downloadRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void userWithCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_REQUESTOR_ROLE__CASEWORKER, caseDetails.getData())
            .then()
            .statusCode(403);
    }

    @Test
    public void userWithCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_NONACCESS_ROLE__CITIZEN, caseDetails.getData())
            .then()
            .statusCode(403);
    }
}
