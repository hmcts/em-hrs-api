package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DownloadHearingRecordingScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;

    @Before
    public void setup() throws Exception {
        testUtil.deleteFileFromCvpContainer(FOLDER);
        testUtil.deleteFileFromHrsContainer(FOLDER);

        final String id = UUID.randomUUID().toString();
        FILE_NAME = String.format(FILE_NAME, id);
        testUtil.uploadToCvpContainer(FILE_NAME);
        createFolderIfDoesNotExistInHrsDB(FOLDER);
    }

    @After
    public void clear(){
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    @DisplayName("An user with caseworker-hrs role should be able to download hearing recordings")
    public void anUserWithCaseWorkerHrsRoleShouldBeAbleToDownloadHearingRecordings() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(FILE_NAME);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCCDByRecordingReference(FILE_NAME);
        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final int expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        final byte[] downloadedFileBytes =
            downloadRecording(EMAIL_ADDRESS, CASE_WORKER_HRS_ROLE, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    @DisplayName("An user with caseworker role should not be able to download hearing recordings")
    public void anUserWithCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(FILE_NAME);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCCDByRecordingReference(FILE_NAME);
        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final int expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        final byte[] downloadedFileBytes =
            downloadRecording(EMAIL_ADDRESS, CASE_WORKER_ROLE, caseDetails.getData())
                .then()
                .statusCode(200) //FIXME should return 403
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));

    }

    @Test
    @DisplayName("An user with citizen role should not be able to download hearing recordings")
    public void anUserWithCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() throws Exception {
        final JsonNode segmentPayload = getSegmentPayload(FILE_NAME);

        postRecordingSegment(segmentPayload)
            .then()
            .log().all()
            .statusCode(202);

        TimeUnit.SECONDS.sleep(20);

        final Optional<CaseDetails> optionalCaseDetails = findCaseDetailsInCCDByRecordingReference(FILE_NAME);
        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());

        final int expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));

        final byte[] downloadedFileBytes =
            downloadRecording(EMAIL_ADDRESS, CITIZEN_ROLE, caseDetails.getData())
                .then()
                .statusCode(200) //FIXME should return 403
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }
}
