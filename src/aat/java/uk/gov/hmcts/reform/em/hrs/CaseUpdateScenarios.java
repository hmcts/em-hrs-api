package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper.HRS_TESTER;

public class CaseUpdateScenarios extends BaseTest {

    private static final String FOLDER = "audiostream115";
    private static final String RECORDING_REF = FOLDER + "/FM-0111-testfile200M_2020-01-01-11.11.11.123-UTC_0.mp4";

    @Value("${azure.storage.cvp.container-url}")
    private String cvpContainerUrl;

    @Test
    public void testCcdCaseUpdate() {
        getFilenames(FOLDER)
            .then()
            .statusCode(200);

        JsonNode reqBody = createRecordingSegment(
            FOLDER,
            cvpContainerUrl + RECORDING_REF,
            RECORDING_REF,
            "mp4",
            0,
            "2020-01-01"
        );

        postRecordingSegment(reqBody)
            .then()
            .statusCode(202);
    }

    @Ignore
    @Test
    public void testDocumentShare() {
        CaseDetails caseDetails = searchForCase(RECORDING_REF).orElseThrow();

        shareRecording("sharee@email.com", caseDetails)
            .then()
            .statusCode(200);
    }

    @Ignore
    @Test
    public void testRecordingDownload() {
        CaseDetails caseDetails = searchForCase(RECORDING_REF).orElseThrow();

        downloadRecording(HRS_TESTER, caseDetails.getData())
            .then()
            .statusCode(200);
    }
}
