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

import java.io.IOException;
import java.security.SecureRandom;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    protected static final String JURISDICTION = "HRS";
    protected static final String LOCATION_CODE = "0123";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String BEARER = "Bearer ";
    protected static final String FILE_EXT = "mp4";

    public static String SYSUSER_HRSAPI_USER = "emhrsapi@test.internal";
    public static List<String> SYSUSER_HRSAPI_USER_ROLES = List.of("caseworker", "caseworker-hrs-searcher", "ccd-import");

    protected static final String USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS = "em-test-caseworker-hrs-searcher@test.internal";
    protected static final String USER_WITH_REQUESTOR_ROLE__CASEWORKER = "em-test-caseworker@test.internal";
    protected static final String USER_WITH_NONACCESS_ROLE__CITIZEN = "em-test-citizen@test.internal";
    protected static final String EMAIL_ADDRESS_INVALID_FORMAT = "invalid@emailaddress";

    protected static final String FOLDER = "audiostream123455";
    protected static final String TIME = "2020-11-04-14.56.32.819";
    public static final String CASEREF_PREFIX = "FUNCTEST_";
    protected static List<String> CASE_WORKER_ROLE = List.of("caseworker");
    protected static List<String> CASE_WORKER_HRS_ROLE = List.of("caseworker", "caseworker-hrs-searcher");
    protected static List<String> CITIZEN_ROLE = List.of("citizen");
    protected static final String CLOSE_CASE = "closeCase";

    int FIND_CASE_TIMEOUT = 30;

    protected String idamAuthHrsTester;
    protected String s2sAuth;
    protected String userIdHrsTester;


    //yyyy-MM-dd---HH-MM-ss---SSS=07-30-2021---16-07-35---485
    DateTimeFormatter datePartFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timePartFormatter = DateTimeFormatter.ofPattern("HH-MM-ss---SSS");

    @Rule
    public RetryRule retryRule = new RetryRule(3);//3 is standard across hmcts projects


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
        LOGGER.info("BASE TEST POST CONSTRUCT INITIALISATIONS....");
        SerenityRest.useRelaxedHTTPSValidation();

        createUserIfNotExists(SYSUSER_HRSAPI_USER, SYSUSER_HRSAPI_USER_ROLES);

        createUserIfNotExists(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, CASE_WORKER_HRS_ROLE);
        createUserIfNotExists(USER_WITH_REQUESTOR_ROLE__CASEWORKER, CASE_WORKER_ROLE);
        createUserIfNotExists(USER_WITH_NONACCESS_ROLE__CITIZEN, CITIZEN_ROLE);

        idamAuthHrsTester = idamHelper.authenticateUser(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);
        s2sAuth = BEARER + s2sHelper.getS2sToken();
        userIdHrsTester = idamHelper.getUserId(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);

        try {
            extendedCcdHelper.importDefinitionFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        return authRequest(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);
    }


    private RequestSpecification authRequest(String username) {
        String userToken = idamAuthHrsTester;
        if (!USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS.equals(username)) {
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

    protected ValidatableResponse getFilenamesCompletedOrInProgress(String folder) {
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
        getFilenamesCompletedOrInProgress(folderName)
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

        LOGGER.info("Found case - id: {}", caseDetails.getId());
        return caseDetails;
    }

    protected Optional<CaseDetails> searchForCase(String caseRef) {
        Map<String, String> searchCriteria = Map.of("case.recordingReference", caseRef);
        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(SYSUSER_HRSAPI_USER, "4590fgvhbfgbDdffm3lk4j");
        String uid = idamClient.getUserInfo(userToken).getUid();

        LOGGER.info("searching for case by ref ({}) with userToken ({}) and serviceToken ({})",
                    caseRef, idamAuthHrsTester.substring(0, 12), s2sToken.substring(0, 12)
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
        String userToken = idamClient.getAccessToken(SYSUSER_HRSAPI_USER, "4590fgvhbfgbDdffm3lk4j");
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


    /**
     * below three methods denerate a random valid UID.
     * ripped from uk.gov.hmcts.ccd.domain.service.common TODO consider importing this as a library if it exists
     *
     * @return A randomly generated, valid, UID.
     */
    public String generateUid() {
        SecureRandom random = new SecureRandom();
        String currentTime10OfSeconds = String.valueOf(System.currentTimeMillis()).substring(0, 11);
        StringBuilder builder = new StringBuilder(currentTime10OfSeconds);
        for (int i = 0; i < 4; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }
        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = checkSum(builder.toString(), true);
        builder.append(checkDigit);

        return builder.toString();
    }

    /**
     * Generate check digit for a number string.
     *
     * @param numberString number string to process
     * @param noCheckDigit Whether check digit is present or not. True if no check Digit
     *                     is appended.
     * @return
     */
    public int checkSum(String numberString, boolean noCheckDigit) {
        int sum = 0;
        int checkDigit = 0;

        if (!noCheckDigit) {
            numberString = numberString.substring(0, numberString.length() - 1);
        }

        boolean isDouble = true;
        for (int i = numberString.length() - 1; i >= 0; i--) {
            int k = Integer.parseInt(String.valueOf(numberString.charAt(i)));
            sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
            isDouble = !isDouble;
        }

        if ((sum % 10) > 0) {
            checkDigit = (10 - (sum % 10));
        }

        return checkDigit;
    }


    private int sumToSingleDigit(int k) {
        if (k < 10) {
            return k;
        }

        return sumToSingleDigit(k / 10) + (k % 10);
    }
}
