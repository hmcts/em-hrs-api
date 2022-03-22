package uk.gov.hmcts.reform.em.hrs;

import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShareScenarios extends BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShareScenarios.class);

    @Autowired
    private BlobUtil blobUtil;

    private String caseRef;
    private String filename;
    private Set<String> filenames = new HashSet<String>();
    private CaseDetails caseDetails;
    private int expectedFileSize;

    private Long ccdCaseId;

    @PostConstruct
    public void setup() throws Exception {
        LOGGER.info("SETTING UP SHARE RECORDING SCENARIOS....");

        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = timebasedCaseRef();
        filename = filename(caseRef, 0);
        filenames.add(filename);


        LOGGER.info("SET UP: UPLOADING TO CVP");
        blobUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.cvpBlobContainerClient);

        LOGGER.info("SET UP: POSTING TO HRS");
        postRecordingSegment(caseRef, 0).then().statusCode(202);
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.hrsBlobContainerClient);

        LOGGER.info("SET UP: CHECKING CASE IN CCD");
        caseDetails = findCaseWithAutoRetryWithUserWithSearcherRole(caseRef);
        ccdCaseId = caseDetails.getId();
        //used in tests to verify file is fully downloaded
        LOGGER.info("SET UP: CHECKING FILE SIZE UPLOADED TO CVP");
        expectedFileSize = blobUtil.getFileFromPath("data/test_data.mp4").readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        LOGGER.info("SET UP: SCENARIO DATA READY FOR TESTING");

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
        // NOTE THAT ANY EMAIL ADDRESS THAT IS SHARED TO IS ABLE TO DOWNLOAD FROM HRS AS LONG AS THEY ARE IN IDAM
        // HOWEVER TO ACCESS THE FILE, THEY HAVE TO DOWNLOAD VIA EXUI
        // WHICH AT TIME OF WRITING ONLY ALLOWS CASEWORKER AND CASEWORKER_HRS ROLES FOR THE HRS JURISDICTION
        // TODO NOT SURE WHY THIS TEST IS HERE - POSSIBLY FUTURE PROOFING - DOES CITIZEN ROLE ACTUALLY EXIST IN IDAM?
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

    @Test
    public void shouldReturn400WhenShareHearingRecordingsToInvalidEmailAddress() {
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, EMAIL_ADDRESS_INVALID_FORMAT);

        shareRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, callbackRequest)
            .then().log().all()
            .statusCode(400);
    }

    @Test
    public void shouldReturn404WhenShareHearingRecordingsToEmailAddressWithNonExistentCaseId() {
        Long randomCcdId = Long.valueOf(generateUid());
        caseDetails.setId(randomCcdId);
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY);
        LOGGER.info(
            "Sharing case with new timebased random ccd id {}, by user {}",
            randomCcdId,
            USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS
        );
        shareRecording(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, callbackRequest)
            .then().log().all()
            .statusCode(404);

        caseDetails.setId(null);
    }

    @AfterAll
    void clearUp(){
        if(closeCcdCase) {
            LOGGER.info("Closing CCD case, case id {}", ccdCaseId);
            extendedCcdHelper.closeCcdCase(ccdCaseId);
        }
    }
}
