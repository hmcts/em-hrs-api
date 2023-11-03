package uk.gov.hmcts.reform.em.hrs;

import jakarta.annotation.PostConstruct;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class DownloadNonSharedScenarios extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadNonSharedScenarios.class);

    private String filename;
    private Set<String> filenames = new HashSet<String>();
    @Autowired
    private BlobUtil blobUtil;
    private String caseRef;
    private CaseDetails caseDetails;
    private int expectedFileSize;

    @PostConstruct
    public void setup() throws Exception {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = timebasedCaseRef();
        filename = filename(caseRef, 0);
        filenames.add(filename);

        LOGGER.info("Priming CVP Container");
        blobUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.cvpBlobContainerClient);

        LOGGER.info("Priming HRS API With Posted Segments");
        postRecordingSegment(caseRef, 0).then().statusCode(202);
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.hrsCvpBlobContainerClient);


        LOGGER.info("Checking CCD and populating default caseDetails");
        caseDetails = findCaseWithAutoRetryWithUserWithSearcherRole(caseRef);

        //used in tests to verify file is fully downloaded
        expectedFileSize = blobUtil.getFileFromPath("data/test_data.mp4").readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));
    }

    @Test
    public void userWithCaseWorkerHrsSearcherRoleShouldBeAbleToDownloadHearingRecordings() {
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
    public void userWithOnlyCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY, caseDetails.getData())
            .then()
            .statusCode(403);
    }

    @Test
    public void userWithOnlyCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_NONACCESS_ROLE__CITIZEN, caseDetails.getData())
            .then()
            .statusCode(403);
    }
}
