//package uk.gov.hmcts.reform.em.hrs;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import org.apache.commons.io.IOUtils;
//import org.junit.Ignore;
//import org.junit.Test;
//import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.StandardCopyOption;
//
//import static uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper.HRS_TESTER;
//
//public class CaseUpdateScenarios extends BaseTest {
//
//    private static final String FOLDER = "functionaltest001";
//    private static final String JURISDICTION_CODE = "FT";
//    private static final String LOCATION_CODE = "0111";
//    private static final String CASE_REF = "testfile200M";
//    private static final String RECORDING_TIME = "2020-01-01-11.11.11.123";
//    private static final int SEGMENT = 0;
//    private static final String FILE_EXT = "mp4";
//    //functionaltest001/FT-0111-testfile200M_2020-01-01-11.11.11.123-UTC_0.mp4
//
//    @Test
//    public void testCcdCaseUpdate() {
//        getFilenames(FOLDER)
//            .then()
//            .statusCode(200);
//
//        JsonNode reqBody = createRecordingSegment(FOLDER, JURISDICTION_CODE, LOCATION_CODE, CASE_REF,
//                                                  RECORDING_TIME, SEGMENT, FILE_EXT);
//
//        postRecordingSegment(reqBody)
//            .then()
//            .statusCode(202);
//    }
//
//    @Ignore
//    @Test
//    public void testDocumentShare() {
//        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();
//
//        shareRecording("sharee@email.com", caseDetails)
//            .then()
//            .statusCode(200);
//    }
//
//    @Ignore
//    @Test
//    public void testRecordingDownload() throws IOException {
//        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();
//
//        InputStream downloadInputStream = downloadRecording(HRS_TESTER, caseDetails.getData()).asInputStream();
//
//        File targetFile = new File("FT-0111-testfile200M_2020-01-01-11.11.11.123-UTC_0.mp4");
//
//        java.nio.file.Files.copy(
//            downloadInputStream,
//            targetFile.toPath(),
//            StandardCopyOption.REPLACE_EXISTING);
//
//        IOUtils.close(downloadInputStream);
//    }
//}
