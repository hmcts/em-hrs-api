

package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.hrs.testutil.*;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper.HRS_TESTER;
import static uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper.HRS_TESTER_ROLES;

@SpringBootTest(classes = {
    ExtendedCcdHelper.class,
    EmTestConfig.class,
    CcdAuthTokenGeneratorConfiguration.class,
    AuthTokenGeneratorConfiguration.class,
    TestUtil.class,
    HrsAzureClient.class
})

@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
@WithTags({@WithTag("testType:Functional")})
public abstract class BaseTest {

    protected static final String JURISDICTION = "HRS";
    protected static final String LOCATION_CODE = "0123";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String BEARER = "Bearer ";
    protected AtomicInteger counter = new AtomicInteger(0);
    protected static final String FILE_EXT = "mp4";
    protected static final String SHAREE_EMAIL_ADDRESS = "sharee@email.com";
    protected static final String EMAIL_ADDRESS = "testuser@email.com";
    protected static final String ERROR_SHAREE_EMAIL_ADDRESS = "sharee.testertest.com";
    protected static final int SEGMENT = 0;
    protected static final String FOLDER = "audiostream123456";
    protected static final Random rd = new Random();
    protected static final int random = Math.abs(rd.nextInt());
    protected static final String CASEREF = "FUNCTEST" + random;
    protected static final String TIME =  "2020-11-04-14.56.32.819";
    protected String fileName = FOLDER + "/" + JURISDICTION + "-" + LOCATION_CODE + "-" + CASEREF + "_" + TIME + "-UTC_"
        + SEGMENT + ".mp4";
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_ROLE = List.of("caseworker-hrs");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");

    protected String idamAuth;
    protected String s2sAuth;
    protected String userId;

    @Rule
    public RetryRule retryRule = new RetryRule(1);

    @Value("${test.url}")
    protected String testUrl;

    @Value("${azure.storage.cvp.container-url}")
    private String cvpContainerUrl;

    @Autowired
    protected IdamHelper idamHelper;

    @Autowired
    protected S2sHelper s2sHelper;

    @Autowired
    protected CoreCaseDataApi coreCaseDataApi;

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @PostConstruct
    public void init() {
        SerenityRest.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser(HRS_TESTER);
        s2sAuth = BEARER + s2sHelper.getS2sToken();
        userId = idamHelper.getUserId(HRS_TESTER);
    }


    public RequestSpecification authRequest() {
        return authRequest(HRS_TESTER, HRS_TESTER_ROLES);
    }


    private RequestSpecification authRequest(String username, List<String> roles) {
        String userToken = idamAuth;
        if (!HRS_TESTER.equals(username)) {
            idamHelper.createUser(username, roles);
            userToken = idamHelper.authenticateUser(username);
        }
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header("Authorization", userToken)
            .header("ServiceAuthorization", s2sAuth);
    }

    public RequestSpecification s2sAuthRequest() {
        return SerenityRest
            .given()
            .header("ServiceAuthorization", s2sAuth);
    }

    protected ValidatableResponse getFilenames(String folder) {
        return authRequest()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get("/folders/" + folder)
            .then();
    }

    protected Response postRecordingSegment(JsonNode reqBody) {
        return s2sAuthRequest()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when()
            .post("/segments");
    }

    protected Response shareRecording(String email, List<String> roles, CallbackRequest callbackRequest) {
        return authRequest(email, roles)
            .body(callbackRequest)
            .when().log().all()
            .post("/sharees");
    }

    protected Response downloadRecording(String email, List<String> roles, Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(caseRecordingFile -> caseRecordingFile.getCaseDocument())
            .map(caseDocument -> caseDocument.getBinaryUrl())
            .findFirst()
            .orElseThrow();

        return authRequest(email, roles)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get(recordingUrl);
    }

    protected JsonNode createRecordingSegment(String folder,
                                              String jurisdictionCode, String locationCode, String caseRef,
                                              String recordingTime, int segment, String fileExt) {
        String recordingRef = folder + "/" + jurisdictionCode + "-" + locationCode + "-" + caseRef + "_" + recordingTime;
        String filename = recordingRef + "-UTC_" + segment + "." + fileExt;
        return JsonNodeFactory.instance.objectNode()
            .put("folder", folder)
            .put("recording-ref", recordingRef)
            .put("recording-source","CVP")
            .put("court-location-code",locationCode)
            .put("service-code","PROBATE")
            .put("hearing-room-ref","London")
            .put("jurisdiction-code",jurisdictionCode)
            .put("case-ref",caseRef)
            .put("cvp-file-url", cvpContainerUrl + filename)
            .put("filename", filename)
            .put("filename-extension", fileExt)
            .put("file-size", 200724364L)
            .put("segment", segment)
            .put("recording-date-time", recordingTime);
    }

    protected JsonNode getSegmentPayload(final String fileName) {
        return createRecordingSegment(
            FOLDER,
            JURISDICTION,
            LOCATION_CODE,
            CASEREF,
            TIME,
            SEGMENT,
            FILE_EXT
        );
    }


    protected void createFolderIfDoesNotExistInHrsDB(final String folderName) {
        getFilenames(folderName)
            .log().all()
            .assertThat()
            .statusCode(200);
    }

    protected Optional<CaseDetails> searchForCase(String recordingRef) {
        Map<String, String> searchCriteria = Map.of("case.recordingReference", recordingRef);
        return coreCaseDataApi
            .searchForCaseworker(idamAuth, s2sAuth, userId, JURISDICTION, CASE_TYPE, searchCriteria)
            .stream().findAny();
    }

    protected CallbackRequest getCallbackRequest(final CaseDetails caseDetails, final String emailId) {
        caseDetails.getData().put("recipientEmailAddress", emailId);
        return CallbackRequest.builder().caseDetails(caseDetails).build();
    }

}
