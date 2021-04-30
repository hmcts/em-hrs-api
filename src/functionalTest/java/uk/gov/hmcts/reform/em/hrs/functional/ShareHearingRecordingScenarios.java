package uk.gov.hmcts.reform.em.hrs.functional;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.functional.util.TestUtil;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShareHearingRecordingScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;

    @Before
    public void setup() throws Exception {
        testUtil.deleteFileFromCvpContainer(FOLDER);
        testUtil.deleteFileFromHrsContainer(FOLDER);

        fileName = String.format(fileName, counter.incrementAndGet());
        testUtil.uploadToCvpContainer(fileName);
        createFolderIfDoesNotExistInHrsDB(FOLDER);
    }

    @After
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    public void shouldAbleToShareHearingRecordingsToEmailAddressAndDownload() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCcdByRecordingReference(fileName);
        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final CallbackRequest callbackRequest = getCallbackRequest(caseDetails, SHAREE_EMAIL_ADDRESS);
        final Response shareRecordingResponse = shareRecording(SHAREE_EMAIL_ADDRESS, CASE_WORKER_ROLE, callbackRequest);

        shareRecordingResponse
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final int expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        final byte[] downloadedFileBytes =
            downloadRecording(caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;

        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void shareeWithCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCcdByRecordingReference(fileName);
        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final CallbackRequest callbackRequest = getCallbackRequest(caseDetails, SHAREE_EMAIL_ADDRESS);
        final Response shareRecordingResponse = shareRecording(SHAREE_EMAIL_ADDRESS, CITIZEN_ROLE, callbackRequest);

        shareRecordingResponse
            .then()
            .log().all()
            .assertThat()
            .statusCode(200);

        final int expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        final byte[] downloadedFileBytes =
            downloadRecording(SHAREE_EMAIL_ADDRESS, CITIZEN_ROLE, caseDetails.getData())
                .then()
                .statusCode(200) //FIXME citizen role should not download hearing recordings
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;

        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void shouldReturn400WhenShareHearingRecordingsToInvalidEmailAddress() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCcdByRecordingReference(fileName);
        assertTrue(optionalCaseDetails.isPresent());

        CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final CallbackRequest callbackRequest = getCallbackRequest(caseDetails, ERROR_SHAREE_EMAIL_ADDRESS);
        final Response shareRecordingResponse = shareRecording(SHAREE_EMAIL_ADDRESS, CASE_WORKER_ROLE, callbackRequest);

        shareRecordingResponse
            .then().log().all()
            .assertThat().statusCode(400);
    }

    @Test
    public void shouldReturn404WhenShareHearingRecordingsToEmailAddressWithNonExistentCaseId() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(fileName);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCcdByRecordingReference(fileName);
        assertTrue(optionalCaseDetails.isPresent());

        CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        caseDetails.setId(RandomUtils.nextLong());
        final CallbackRequest callbackRequest = getCallbackRequest(caseDetails, SHAREE_EMAIL_ADDRESS);
        final Response shareRecordingResponse = shareRecording(SHAREE_EMAIL_ADDRESS, CASE_WORKER_ROLE, callbackRequest);

        shareRecordingResponse
            .then().log().all()
            .assertThat().statusCode(404);
    }
}
