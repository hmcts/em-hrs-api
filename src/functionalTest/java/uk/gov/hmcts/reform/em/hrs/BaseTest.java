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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.hrs.testutil.AuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.hrs.testutil.HrsAzureClient;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    protected static final String JURISDICTION = "HRS";
    protected static final String LOCATION_CODE = "0123";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String BEARER = "Bearer ";
    protected static final String FILE_EXT = "mp4";
    protected static final String SHAREE_EMAIL_ADDRESS = "sharee@email.com";
    protected static final String CASEWORKER_HRS_USER = "em.hrs.api@hmcts.net.internal";
    protected static final String CASEWORKER_USER = "hearing.audio.requester@gmail.com";
    protected static final String CITIZEN_USER = "citizen.role@outlook.com";
    protected static final String ERROR_SHAREE_EMAIL_ADDRESS = "sharee.testertest.com";
    protected static final int SEGMENT = 0;
    protected static final String FOLDER = "audiostream123455";
    protected static final String TIME = "2020-11-04-14.56.32.819";
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_ROLE = List.of("caseworker-hrs");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");
    protected static final String CLOSE_CASE = "closeCase";

    protected String idamAuth;
    protected String s2sAuth;
    protected String userId;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Value("${test.url}")
    protected String testUrl;

    @Value("${azure.storage.cvp.container-url}")
    private String cvpContainerUrl;

    @Autowired
    protected IdamClient idamClient;

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

    protected Response postRecordingSegment(String caseRef) {
        final JsonNode segmentPayload = createSegmentPayload(caseRef);
        return postRecordingSegment(segmentPayload);
    }

    protected Response postRecordingSegment(JsonNode segmentPayload) {
        return s2sAuthRequest()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(segmentPayload)
            .when().log().all()
            .post("/segments");
    }

    protected Response shareRecording(String email, List<String> roles, CallbackRequest callbackRequest) {
        JsonNode reqBody = new ObjectMapper().convertValue(callbackRequest, JsonNode.class);
        return authRequest(email, roles)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
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

    protected JsonNode createSegmentPayload(String caseRef) {
        return createRecordingSegment(
            FOLDER,
            JURISDICTION,
            LOCATION_CODE,
            caseRef,
            TIME,
            SEGMENT,
            FILE_EXT
        );
    }

    protected JsonNode createRecordingSegment(String folder,
                                              String jurisdictionCode, String locationCode, String caseRef,
                                              String recordingTime, int segment, String fileExt) {
        String recordingRef =
            folder + "/" + jurisdictionCode + "-" + locationCode + "-" + caseRef + "_" + recordingTime;
        String filename = recordingRef + "-UTC_" + segment + "." + fileExt;
        return JsonNodeFactory.instance.objectNode()
            .put("folder", folder)
            .put("recording-ref", recordingRef)
            .put("recording-source", "CVP")
            .put("court-location-code", locationCode)
            .put("service-code", "PROBATE")
            .put("hearing-room-ref", "London")
            .put("jurisdiction-code", jurisdictionCode)
            .put("case-ref", caseRef)
            .put("cvp-file-url", cvpContainerUrl + filename)
            .put("filename", filename)
            .put("filename-extension", fileExt)
            .put("file-size", 200724364L)
            .put("segment", segment)
            .put("recording-date-time", recordingTime);
    }

    protected void createFolderIfDoesNotExistInHrsDB(final String folderName) {
        getFilenames(folderName)
            .log().all()
            .assertThat()
            .statusCode(200);
    }

    protected CaseDetails findCase(String caseRef) throws InterruptedException {
        LOGGER.info("CaseRef {},", caseRef);
        Optional<CaseDetails> optionalCaseDetails = searchForCase(caseRef);

        int count = 0;
        while (count <= 10 && optionalCaseDetails.isEmpty()) {
            TimeUnit.SECONDS.sleep(30);
            optionalCaseDetails = searchForCase(caseRef);
            count++;
        }

        assertTrue(optionalCaseDetails.isPresent());

        final CaseDetails caseDetails = optionalCaseDetails.orElseGet(() -> CaseDetails.builder().build());
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getId());
        assertNotNull(caseDetails.getData());
        return caseDetails;
    }

    protected Optional<CaseDetails> searchForCase(String caseRef) {
        Map<String, String> searchCriteria = Map.of("case.recordingReference", caseRef);
        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(HRS_TESTER, "4590fgvhbfgbDdffm3lk4j");
        String uid = idamClient.getUserInfo(userToken).getUid();

        LOGGER.info("searching for case with userToken ({}) and serviceToken ({})",
                    idamAuth.substring(0, 12), s2sToken.substring(0, 12)
        );
        return coreCaseDataApi
            .searchForCaseworker(userToken, s2sToken, uid,
                                 JURISDICTION, CASE_TYPE, searchCriteria
            )
            .stream().findAny();
    }

    protected CallbackRequest createCallbackRequest(final CaseDetails caseDetails, final String emailId) {
        caseDetails.getData().put("recipientEmailAddress", emailId);
        caseDetails.setCreatedDate(null);
        caseDetails.setLastModified(null);
        return CallbackRequest.builder().caseDetails(caseDetails).build();
    }

    protected String timebasedCaseRef() {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-MM-ss-SSS");
        //yyyy-MM-dd-HH-MM-ss-SSS=07-30-2021-16-07-35-485
        ZonedDateTime now = ZonedDateTime.now();

        String time = customFormatter.format(now);
        return "FUNCTEST_" + time;
    }

    protected String filename(String caseRef) {
        return FOLDER + "/" + JURISDICTION + "-" + LOCATION_CODE + "-" + caseRef + "_" + TIME
            + "-UTC_" + SEGMENT + ".mp4";
    }

    public String closeCase(final String caseRef, CaseDetails caseDetails) {

        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(HRS_TESTER, "4590fgvhbfgbDdffm3lk4j");
        String uid = idamClient.getUserInfo(userToken).getUid();

        StartEventResponse startEventResponse =
            coreCaseDataApi.startEvent(userToken, s2sToken, String.valueOf(caseDetails.getId()), CLOSE_CASE);

        LOGGER.info("closing case ({}) with reference ({}), right now it has state ({})",
                    caseDetails.getId(), caseRef, caseDetails.getState()
        );

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .caseReference(caseRef)
            .build();

        caseDetails = coreCaseDataApi
            .submitEventForCaseWorker(userToken, s2sToken, uid,
                                      JURISDICTION, CASE_TYPE, String.valueOf(caseDetails.getId()), false,
                                      caseData
            );

        assert (caseDetails.getState().equals("1_CLOSED"));
        LOGGER.info("closed case ({}) with reference ({}), it now has state ({})",
                    caseDetails.getId(), caseRef, caseDetails.getState()
        );
        return caseDetails.getState();
    }
}
