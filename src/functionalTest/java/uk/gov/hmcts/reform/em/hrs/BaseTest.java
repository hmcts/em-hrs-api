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
import uk.gov.hmcts.reform.em.hrs.testutil.AzureStorageContainerClientBeans;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.hrs.testutil.SleepHelper;
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
import javax.annotation.PostConstruct;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {
    ExtendedCcdHelper.class,
    EmTestConfig.class,
    CcdAuthTokenGeneratorConfiguration.class,
    AuthTokenGeneratorConfiguration.class,
    BlobUtil.class,
    AzureStorageContainerClientBeans.class
})

@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringJUnit4ClassRunner.class)
@WithTags({@WithTag("testType:Functional")})
public abstract class BaseTest {

    static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    protected static final String JURISDICTION = "HRS";
    protected static final String LOCATION_CODE = "0123";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String BEARER = "Bearer ";
    protected static final String FILE_EXT = "mp4";
    protected static final String USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS = "em-test-caseworker-hrs@test.internal";
    protected static final String USER_WITH_REQUESTOR_ROLE__CASEWORKER = "em-test-caseworker@test.internal";
    protected static final String USER_WITH_NONACCESS_ROLE__CITIZEN = "em-test-citizen@test.internal";
    protected static final String EMAIL_ADDRESS_INVALID_FORMAT = "invalid@emailaddress";
    protected static final String FOLDER = "audiostream123455";
    protected static final String TIME = "2020-11-04-14.56.32.819";
    public static final String CASEREF_PREFIX = "FUNCTEST_";
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_ROLE = List.of("caseworker", "caseworker-hrs");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");
    protected static final String CLOSE_CASE = "closeCase";

    public static String HRS_TESTER = "hrs.test.user@hmcts.net";
    public static List<String> HRS_TESTER_ROLES = List.of("caseworker", "caseworker-hrs", "ccd-import");

    int FIND_CASE_TIMEOUT = 30;

    protected String idamAuth_hrs_tester;
    protected String s2sAuth;
    protected String userId_hrs_tester;


    //yyyy-MM-dd---HH-MM-ss---SSS=07-30-2021---16-07-35---485
    DateTimeFormatter datePartFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timePartFormatter = DateTimeFormatter.ofPattern("HH-MM-ss---SSS");

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

        createUserIfNotExists(HRS_TESTER, HRS_TESTER_ROLES);
        createUserIfNotExists(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, CASE_WORKER_HRS_ROLE);
        createUserIfNotExists(USER_WITH_REQUESTOR_ROLE__CASEWORKER, CASE_WORKER_ROLE);
        createUserIfNotExists(USER_WITH_NONACCESS_ROLE__CITIZEN, CITIZEN_ROLE);

        idamAuth_hrs_tester = idamHelper.authenticateUser(HRS_TESTER);
        s2sAuth = BEARER + s2sHelper.getS2sToken();
        userId_hrs_tester = idamHelper.getUserId(HRS_TESTER);


    }

    private void createUserIfNotExists(String email, List<String> roles) {
        /* in some cases, there were conflicts between PR branches being built
        due to users being deleted / recreated

        as the roles are static they do not need to be deleted each time
        should the roles change for users, then the recreateUsers flag will need to be true before merging to master
         */

        boolean recreateUsers = false;

        if (recreateUsers) {
            idamHelper.createUser(email, roles);
        } else {
            try {
                idamHelper.getUserId(email);
            } catch (Exception e) {//if user does not exist
                idamHelper.createUser(email, roles);
            }
        }
    }


    public RequestSpecification authRequest() {
        return authRequest(HRS_TESTER);
    }


    private RequestSpecification authRequest(String username) {
        String userToken = idamAuth_hrs_tester;
        if (!HRS_TESTER.equals(username)) {
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

    protected Response postRecordingSegment(String caseRef, int segment) {
        final JsonNode segmentPayload = createSegmentPayload(caseRef, segment);
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

    protected Response shareRecording(String email, CallbackRequest callbackRequest) {
        JsonNode reqBody = new ObjectMapper().convertValue(callbackRequest, JsonNode.class);
        return authRequest(email)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .post("/sharees");
    }

    protected Response downloadRecording(String email, Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(caseRecordingFile -> caseRecordingFile.getCaseDocument())
            .map(caseDocument -> caseDocument.getBinaryUrl())
            .findFirst()
            .orElseThrow();

        return authRequest(email)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get(recordingUrl);
    }

    protected JsonNode createSegmentPayload(String caseRef, int segment) {
        return createRecordingSegment(
            FOLDER,
            JURISDICTION,
            LOCATION_CODE,
            caseRef,
            TIME,
            segment,
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

    CaseDetails findCaseWithAutoRetry(String caseRef) {
        Optional<CaseDetails> optionalCaseDetails = searchForCase(caseRef);

        int count = 0;
        while (count <= 10 && optionalCaseDetails.isEmpty()) {
            SleepHelper.sleepForSeconds(FIND_CASE_TIMEOUT);
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

        LOGGER.info("searching for case by ref ({}) with userToken ({}) and serviceToken ({})",
                    caseRef, idamAuth_hrs_tester.substring(0, 12), s2sToken.substring(0, 12)
        );
        return coreCaseDataApi
            .searchForCaseworker(userToken, s2sToken, uid,
                                 JURISDICTION, CASE_TYPE, searchCriteria
            )
            .stream().findAny();
    }

    protected CallbackRequest addEmailRecipientToCaseDetailsCallBack(final CaseDetails caseDetails,
                                                                     final String emailId) {
        caseDetails.getData().put("recipientEmailAddress", emailId);
        caseDetails.setCreatedDate(null);
        caseDetails.setLastModified(null);
        return CallbackRequest.builder().caseDetails(caseDetails).build();
    }

    protected String timebasedCaseRef() {

        ZonedDateTime now = ZonedDateTime.now();

        String date = datePartFormatter.format(now);
        String time = timePartFormatter.format(now);
        //yyyy-MM-dd---HH-MM-ss---SSS=07-30-2021---16-07-35---485
        return CASEREF_PREFIX + date + "---" + time;
    }

    protected String filename(String caseRef, int segment) {
        return FOLDER + "/" + JURISDICTION + "-" + LOCATION_CODE + "-" + caseRef + "_" + TIME
            + "-UTC_" + segment + ".mp4";
    }

    public String closeCase(final String caseRef, CaseDetails caseDetails) {

        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(HRS_TESTER, "4590fgvhbfgbDdffm3lk4j");
        String uid = idamClient.getUserInfo(userToken).getUid();

        StartEventResponse startEventResponse =
            coreCaseDataApi.startEvent(userToken, s2sToken, String.valueOf(caseDetails.getId()), CLOSE_CASE);

        LOGGER.info("closing case id ({}) with reference ({}), right now it has state ({})",
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
        LOGGER.info("closed case id ({}) with reference ({}), it now has state ({})",
                    caseDetails.getId(), caseRef, caseDetails.getState()
        );
        return caseDetails.getState();
    }


}
