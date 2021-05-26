package uk.gov.hmcts.reform.em.hrs.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.functional.config.AuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.functional.config.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.functional.config.HrsAzureClient;
import uk.gov.hmcts.reform.em.hrs.functional.util.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.hrs.functional.util.TestUtil;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.hrs.functional.util.ExtendedCcdHelper.HRS_TESTER;


@SpringBootTest(classes = {
    ExtendedCcdHelper.class,
    EmTestConfig.class,
    CcdAuthTokenGeneratorConfiguration.class,
    AuthTokenGeneratorConfiguration.class,
    TestUtil.class,
    HrsAzureClient.class
})
@TestPropertySource("classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public abstract class BaseTest {

    protected AtomicInteger counter = new AtomicInteger(0);
    protected static final String JURISDICTION = "HRS";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String FILE_EXT = "mp4";
    protected static final String SHAREE_EMAIL_ADDRESS = "sharee@email.com";
    protected static final String EMAIL_ADDRESS = "testuser@email.com";
    protected static final String ERROR_SHAREE_EMAIL_ADDRESS = "sharee.testertest.com";
    protected static final int SEGMENT = 0;
    protected static final String FOLDER = "audiostream123456";
    protected String fileName = "FM-0124-BV40D04_2021-11-04-15.58.39.819-UTC_%s.mp4";
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_ROLE = List.of("caseworker-hrs");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");
    public static List<String> HRS_TESTER_ROLES = List.of("caseworker", "caseworker-hrs", "ccd-import");
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
    private TestUtil testUtil;

    @PostConstruct
    public void init() {
        SerenityRest.useRelaxedHTTPSValidation();
        idamHelper.createUser(HRS_TESTER, HRS_TESTER_ROLES);
        idamAuth = idamHelper.authenticateUser(HRS_TESTER);
        s2sAuth = s2sHelper.getS2sToken();
        userId = idamHelper.getUserId(HRS_TESTER);
    }

    protected ValidatableResponse getRecordingFileNames(String folder) {
        return authRequest()
            .when().log().all()
            .get(testUtil.getCvpBlobContainerClient().getBlobContainerUrl() + "/" + FOLDER + "/" + fileName)
            .then();
    }

    protected Response postRecordingSegment(JsonNode reqBody) {
        return authRequest()
            .body(reqBody)
            .when().log().all()
            .post("/segments");
    }

    protected Response shareRecording(String email, List<String> roles, CallbackRequest callbackRequest) {
        return authRequest(email, roles)
            .body(callbackRequest)
            .when().log().all()
            .post("/sharees");
    }

    protected Response downloadRecording(Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(caseRecordingFile -> caseRecordingFile.getCaseDocument())
            .map(caseDocument -> caseDocument.getBinaryUrl())
            .findFirst()
            .orElseThrow();

        return authRequest()
            .when().log().all()
            .get(recordingUrl);
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
            .when().log().all()
            .get(recordingUrl);
    }

    protected Optional<CaseDetails> findCaseDetailsInCcdByRecordingReference(String recordingRef) {
        Map<String, String> searchCriteria = Map.of("case.recordingReference", recordingRef);
        return coreCaseDataApi
            .searchForCaseworker(idamAuth, s2sAuth, userId, JURISDICTION, CASE_TYPE, searchCriteria)
            .stream().findAny();
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

    protected JsonNode createRecordingSegmentPayload(String folder, String url,
                                                     String filename, String fileExt,
                                                     int segment, String recordingTime) {
        return JsonNodeFactory.instance.objectNode()
            .put("folder", folder)
            .put("recording-ref", filename)
            .put("recording-source", "CVP")
            .put("court-location-code", "London")
            .put("service-code", "PROBATE")
            .put("hearing-room-ref", "12")
            .put("jurisdiction-code", "HRS")
            .put("case-ref", "hearing-12-family-probate-morning")
            .put("cvp-file-url", url)
            .put("filename", filename)
            .put("filename-extension", fileExt)
            .put("file-size", 226200L)
            .put("segment", segment)
            .put("recording-date-time", recordingTime);
    }


    protected CallbackRequest getCallbackRequest(final CaseDetails caseDetails, final String emailId) {
        caseDetails.getData().put("recipientEmailAddress", emailId);
        return CallbackRequest.builder().caseDetails(caseDetails).build();
    }

    protected JsonNode getSegmentPayload(final String fileName) {
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return createRecordingSegmentPayload(
            FOLDER,
            testUtil.getCvpBlobContainerClient().getBlobContainerUrl() + "/" + FOLDER + "/" + fileName,
            this.fileName,
            FILE_EXT,
            SEGMENT,
            DATE_FORMAT.format(timestamp)
        );
    }

    protected void createFolderIfDoesNotExistInHrsDB(final String folderName) {
        getRecordingFileNames(folderName)
            .log().all()
            .assertThat()
            .statusCode(200);
    }
}
