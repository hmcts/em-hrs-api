package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class ShareScenarios extends BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShareScenarios.class);
    private BlobUtil blobUtil;
    private Set<String> filenames = new HashSet<>();
    private CaseDetails caseDetails;
    private int expectedFileSize;
    private Long ccdCaseId;

    @Value("${endpoint.deleteCase.enabled}")
    private boolean deleteCaseEndpointEnabled;

    @Autowired
    ShareScenarios(
        IdamClient idamClient,
        IdamHelper idamHelper,
        S2sHelper s2sHelper,
        CoreCaseDataApi coreCaseDataApi,
        ExtendedCcdHelper extendedCcdHelper,
        BlobUtil blobUtil
    ) {
        super(idamClient, idamHelper, s2sHelper, coreCaseDataApi, extendedCcdHelper);
        this.blobUtil = blobUtil;
    }

    @BeforeEach
    public void setup() throws Exception {
        LOGGER.info("SETTING UP SHARE RECORDING SCENARIOS....");

        createFolderIfDoesNotExistInHrsDB(FOLDER);
        String caseRef = timebasedCaseRef();
        String filename = filename(caseRef, 0);
        filenames.add(filename);


        LOGGER.info("SET UP: UPLOADING TO CVP");
        blobUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.cvpBlobContainerClient);

        LOGGER.info("SET UP: POSTING TO HRS");
        postRecordingSegment(caseRef, 0).then().statusCode(202);
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.hrsCvpBlobContainerClient);

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
            USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS
        );
        shareRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void shareesShouldReturn401WhenAuthorizationMissing() {
        final CallbackRequest callbackRequest = addEmailRecipientToCaseDetailsCallBack(
            caseDetails,
            USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS
        );

        JsonNode reqBody = new ObjectMapper().convertValue(callbackRequest, JsonNode.class);
        SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", hrsS2sAuth)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .post("/sharees")
            .then()
            .statusCode(401);
    }

    @Test
    public void shareeWithOnlyCaseworkerRoleShouldBeAbleToDownloadRecordings() {
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY);
        shareRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY, caseDetails.getData())
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
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_NONACCESS_ROLE_CITIZEN);
        shareRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, callbackRequest)
            .then()
            .log().all()
            .statusCode(200);

        final byte[] downloadedFileBytes =
            downloadShareeRecording(USER_WITH_NONACCESS_ROLE_CITIZEN, caseDetails.getData())
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

        shareRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, callbackRequest)
            .then().log().all()
            .statusCode(400);
    }

    @Test
    public void shouldReturn404WhenShareHearingRecordingsToEmailAddressWithNonExistentCaseId() {
        Long randomCcdId = Long.valueOf(generateUid());
        caseDetails.setId(randomCcdId);
        final CallbackRequest callbackRequest =
            addEmailRecipientToCaseDetailsCallBack(caseDetails, USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY);
        LOGGER.info(
            "Sharing case with new timebased random ccd id {}, by user {}",
            randomCcdId,
            USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS
        );
        shareRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, callbackRequest)
            .then().log().all()
            .statusCode(404);

        caseDetails.setId(null);
    }

    @Test
    public void shouldReturn204WhenDeletingCaseHearingRecording() {
        Assumptions.assumeTrue(deleteCaseEndpointEnabled);
        deleteRecordings(List.of(ccdCaseId))
            .then().log().all()
            .statusCode(204);
    }

    @Test
    public void shouldReturn401WhenDeletingWithS2sInvalid() {
        Assumptions.assumeTrue(deleteCaseEndpointEnabled);
        deleteRecordingsWithInvalidS2S(List.of(ccdCaseId))
            .then().log().all()
            .statusCode(401);
    }

    @Test
    public void shouldReturn403WhenDeletingWithUnauthorisedService() {
        Assumptions.assumeTrue(deleteCaseEndpointEnabled);
        deleteRecordingsWithUnauthorisedS2S(List.of(ccdCaseId))
            .then().log().all()
            .statusCode(403);
    }

    @AfterEach
    public void clearUp() {
        LOGGER.info("closeCcdCase AfterEach ====> {}", closeCcdCase);
        if (closeCcdCase) {
            LOGGER.info("Closing CCD case, case id {}", ccdCaseId);
            extendedCcdHelper.closeCcdCase(ccdCaseId);
        }
    }

}
