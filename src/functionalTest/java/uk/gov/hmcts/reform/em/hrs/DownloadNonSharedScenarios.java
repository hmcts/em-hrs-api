package uk.gov.hmcts.reform.em.hrs;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class DownloadNonSharedScenarios extends BaseTest {


    private String filename;
    @Autowired
    private BlobUtil blobUtil;
    private String caseRef;
    private CaseDetails caseDetails;
    private int expectedFileSize;

    @Before
    public void setup() throws Exception {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = timebasedCaseRef();
        filename = filename(caseRef, 0);

        int cvpBlobCount = blobUtil.getBlobCount(blobUtil.cvpBlobContainerClient, FOLDER);
        blobUtil.uploadToCvpContainer(filename);
        blobUtil.checkIfBlobUploadedToCvp(FOLDER, cvpBlobCount);


        int hrsBlobCount = blobUtil.getBlobCount(blobUtil.hrsBlobContainerClient, FOLDER);
        postRecordingSegment(caseRef, 0).then().statusCode(202);
        blobUtil.checkIfUploadedToHrsStorage(FOLDER, hrsBlobCount);

        caseDetails = findCaseWithAutoRetry(caseRef);

        expectedFileSize = blobUtil.getTestFile().readAllBytes().length;
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
