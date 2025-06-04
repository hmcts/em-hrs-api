package uk.gov.hmcts.reform.em.hrs;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@Ignore
public class ShareScenariosVh extends BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShareScenariosVh.class);

    @Autowired
    private BlobUtil blobUtil;

    private String caseRef;
    private String filename;
    private Set<String> filenames = new HashSet<String>();
    private CaseDetails caseDetails;
    private int expectedFileSize;

    private Long ccdCaseId;

    @BeforeEach
    public void setup() throws Exception {
        LOGGER.info("SETTING UP SHARE RECORDING SCENARIOS....");

        caseRef = timeVhBasedCaseRef();


        int segment = 0;
        UUID hearingRef = UUID.randomUUID();

        filename = vhFileName(caseRef, segment, null, hearingRef);
        filenames.add(filename);


        LOGGER.info("SET UP: UPLOADING TO VH");
        blobUtil.uploadFileFromPathToVhContainer(filename, "data/test_data.mp4");
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.vhBlobContainerClient);

        LOGGER.info("SET UP: POSTING TO HRS");
        postVhRecordingSegment(caseRef, segment, hearingRef, filename).then().statusCode(202);
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.hrsVhBlobContainerClient);

        LOGGER.info("SET UP: CHECKING CASE IN CCD");
        caseDetails = findCaseWithAutoRetryWithUserWithSearcherRole(caseRef);
        ccdCaseId = caseDetails.getId();
        //used in tests to verify file is fully downloaded
        LOGGER.info("SET UP: CHECKING FILE SIZE UPLOADED TO VH");
        expectedFileSize = blobUtil.getFileFromPath("data/test_data.mp4").readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        LOGGER.info("SET UP: SCENARIO DATA READY FOR TESTING");

    }

    @AfterEach
    public void clearUp() {
        LOGGER.info("closeCcdCase AfterEach ====> {}", closeCcdCase);
        if (closeCcdCase) {
            LOGGER.info("Closing CCD case, case id {}", ccdCaseId);
            extendedCcdHelper.closeCcdCase(ccdCaseId);
        }
    }

    @Test
    public void shareeWithCaseworkerHrsSearcherRoleShouldBeAbleToDownloadRecordings() {
        final CallbackRequest callbackRequest = addEmailRecipientToCaseDetailsCallBack(
            caseDetails,
            USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS
        );
        shareRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void shareeWithOnlyCaseworkerRoleShouldBeAbleToDownloadRecordings() {
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY);
        shareRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void shareeWithCitizenRoleIsAbleToDownloadRecordings() {
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_NONACCESS_ROLE__CITIZEN);
        shareRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_NONACCESS_ROLE__CITIZEN, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

}
