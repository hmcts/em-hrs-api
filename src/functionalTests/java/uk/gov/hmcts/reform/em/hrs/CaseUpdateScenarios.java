package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;


public class CaseUpdateScenarios extends BaseTest {

    private static final String FOLDER = "functionaltest001";
    private static final String JURISDICTION_CODE = "FT";
    private static final String LOCATION_CODE = "0111";
    private static final String CASE_REF = "testfile200M";
    private static final String RECORDING_TIME = "2020-01-01-11.11.11.123";
    private static final int SEGMENT = 0;
    private static final String FILE_EXT = "mp4";
    //functionaltest001/FT-0111-testfile200M_2020-01-01-11.11.11.123-UTC_0.mp4

    @Ignore
    @Test
    public void testCcdCaseUpdate() {
        getFilenames(FOLDER)
            .assertThat().log().all()
            .statusCode(200);

        JsonNode reqBody = createRecordingSegment(FOLDER, JURISDICTION_CODE, LOCATION_CODE, CASE_REF,
                                                  RECORDING_TIME, SEGMENT, FILE_EXT);
        postRecordingSegment(reqBody)
            .then()
            .statusCode(202);
    }

    @Test
    public void testDocumentShare() throws InterruptedException {
        getFilenames(FOLDER).statusCode(200);

        JsonNode reqBody = createRecordingSegment(FOLDER, JURISDICTION_CODE, LOCATION_CODE, CASE_REF,
                                                  RECORDING_TIME, SEGMENT + 1, FILE_EXT);
        postRecordingSegment(reqBody).then().statusCode(202);

        Thread.sleep(300000);

        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();
        final CallbackRequest callbackRequest = getCallbackRequest(caseDetails, SHAREE_EMAIL_ADDRESS);
        shareRecording("sharee@email.com", CASE_WORKER_ROLE, callbackRequest)
            .then()
            .statusCode(200);
    }

    @Ignore
    @Test
    public void testRecordingDownload() throws IOException {
        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();

        InputStream downloadInputStream = downloadRecording(EMAIL_ADDRESS, CITIZEN_ROLE, caseDetails.getData()).asInputStream();

        File targetFile = new File("FT-0111-testfile200M_2020-01-01-11.11.11.123-UTC_0.mp4");

        java.nio.file.Files.copy(
            downloadInputStream,
            targetFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING);

        IOUtils.close(downloadInputStream);
    }
}
