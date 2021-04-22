package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;

import java.io.IOException;
import java.util.Map;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

public class ShareHearingRecordingSceanrios extends BaseTest {

    public static final Long CCD_CASE_ID = 1111111L;
    public static final String SHAREE_EMAIL_ADDRESS = "sharee.tester@test.com";
    public static final String ERROR_SHAREE_EMAIL_ADDRESS = "sharee.testertest.com";

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Test
    @Ignore("The CCD Case Id is not Working as part of a Segment Post")
    public void testShareRecording_success_scenario() throws Exception {

        final CaseDetails request = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS))
            .id(12345L)
            .build();
        sendRequest(request,202);

    }

    @Test
    @Ignore
    public void testShareRecording_negative_non_exitent_ccd_case_id() throws Exception {

        final CaseDetails request = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS))
            .id(11111L)
            .build();
        sendRequest(request,404);

    }

    @Test
    @Ignore("The CCD Case Id is not Working as part of a Segment Post")
    public void testShareRecording_negative_non_existent_email_id() throws Exception {

        final CaseDetails request = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", ERROR_SHAREE_EMAIL_ADDRESS))
            .id(11111L)
            .build();
        sendRequest(request,500);
    }

    private void sendRequest(CaseDetails request,int statusCode) throws IOException {
        authRequest()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonString(request))
            .when()
            .post("/sharees")
            .then()
            .statusCode(statusCode).log().all();
    }

    public static String convertObjectToJsonString(Object object) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        return objectMapper.writeValueAsString(object);
    }
}
