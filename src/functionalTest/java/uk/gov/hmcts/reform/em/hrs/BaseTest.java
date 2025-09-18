package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.hrs.testutil.AuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.AzureStorageContainerClientBeans;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;
import uk.gov.hmcts.reform.em.hrs.testutil.CcdAuthTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.hrs.testutil.SleepHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamConfiguration;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryExtension;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@SpringBootTest(classes = {
    ExtendedCcdHelper.class,
    CcdAuthTokenGeneratorConfiguration.class,
    AuthTokenGeneratorConfiguration.class,
    BlobUtil.class,
    AzureStorageContainerClientBeans.class,
    IdamConfiguration.class,
    EmTestConfig.class
})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith(SpringExtension.class)
@WithTags({@WithTag("testType:Functional")})
@EnableAutoConfiguration
@ComponentScan(basePackages = {
    "uk.gov.hmcts.reform.em.test",
    "uk.gov.hmcts.reform.document"
})
public abstract class BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    protected static final String JURISDICTION = "HRS";
    protected static final String LOCATION_CODE = "0123";
    protected static final String CASE_TYPE = "HearingRecordings";
    protected static final String BEARER = "Bearer ";
    protected static final String FILE_EXT = "mp4";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String DELETE_PATH = "/delete";

    public static final String SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION =
        "hrs.functional.system.user@hmcts.net";

    protected static final String USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS = "em-test-searcher@test.hmcts.net";
    protected static final String USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY = "em-test-requestor@test.hmcts.net";
    protected static final String USER_WITH_NONACCESS_ROLE_CITIZEN = "em-test-citizen@test.hmcts.net";
    protected static final String DUMMY_USER_DEFAULT_PASS =
        "4590fgvhbfgbDdffm3lk4j";//USED ONLY FOR TESTS in IDAM HELPER
    protected static final String EMAIL_ADDRESS_INVALID_FORMAT = "invalid@emailaddress";

    protected static final String FOLDER =
        "audiostream" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    protected static final String DATE = "2020-11-04";
    protected static final String TIME = DATE + "-14.56.32.819";
    public static final String CASEREF_PREFIX = "FUNCTEST_";

    protected static final String CLOSE_CASE = "closeCase";

    protected static final int FIND_CASE_TIMEOUT = 30;

    protected String hrsS2sAuth;

    @Value("${close-ccd-test-cases}")
    protected boolean closeCcdCase;

    //The format "yyyy-MM-dd---HH-MM-ss---SSS" will render "07-30-2021---16-07-35---485"
    private DateTimeFormatter datePartFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timePartFormatter = DateTimeFormatter.ofPattern("HH-MM-ss---SSS");

    @Value("${test.url}")
    protected String testUrl;

    @Value("${azure.storage.cvp.container-url}")
    private String cvpContainerUrl;

    @Value("${idam.hrs-ingestor.user-name}")
    private String idamHrsIngestorUserName;

    @Value("${idam.hrs-ingestor.password}")
    private String idamHrsIngestorPassword;

    protected IdamClient idamClient;

    protected IdamHelper idamHelper;

    protected S2sHelper s2sHelper;

    protected CoreCaseDataApi coreCaseDataApi;

    protected ExtendedCcdHelper extendedCcdHelper;

    @RegisterExtension
    RetryExtension retryExtension = new RetryExtension(3);

    @Autowired
    BaseTest(
        IdamClient idamClient,
        IdamHelper idamHelper,
        S2sHelper s2sHelper,
        CoreCaseDataApi coreCaseDataApi,
        ExtendedCcdHelper extendedCcdHelper
    ) {

        this.idamClient = idamClient;
        this.idamHelper = idamHelper;
        this.s2sHelper = s2sHelper;
        this.coreCaseDataApi = coreCaseDataApi;
        this.extendedCcdHelper = extendedCcdHelper;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("AUTHENTICATING TEST USER FOR CCD CALLS");
        hrsS2sAuth = BEARER + s2sHelper.getS2sToken();
    }

    public RequestSpecification authRequestForSearcherRole() {
        return authRequest(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS);
    }


    public RequestSpecification authRequestForHrsIngestor() {
        LOGGER.info("authRequestForHrsIngestor {}, {}", this.idamHrsIngestorUserName, this.idamHrsIngestorPassword);
        return authRequest(this.idamHrsIngestorUserName, this.idamHrsIngestorPassword);
    }


    private RequestSpecification authRequest(String username, String password) {
        return setJwtTokenHeader(idamHelper.authenticateUser(username, password))
            .header(SERVICE_AUTHORIZATION, hrsS2sAuth);
    }

    private RequestSpecification authRequest(String username) {
        LOGGER.info("authRequestForUsername username {}", username);
        return setJwtTokenHeader(idamHelper.authenticateUser(username))
            .header(SERVICE_AUTHORIZATION, hrsS2sAuth);
    }

    private RequestSpecification userAuthRequest(String username) {
        return setJwtTokenHeader(idamHelper.authenticateUser(username));
    }

    private RequestSpecification setJwtTokenHeader(String userToken) {
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header("Authorization", userToken);
    }

    public RequestSpecification s2sAuthRequest() {
        return SerenityRest
            .given()
            .header(SERVICE_AUTHORIZATION, hrsS2sAuth);
    }

    protected ValidatableResponse getFilenamesCompletedOrInProgress(String folder) {
        return authRequestForHrsIngestor()
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
        return authRequestForHrsIngestor()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(segmentPayload)
            .when().log().all()
            .post("/segments");
    }


    protected Response shareRecording(String sharerUserName, CallbackRequest callbackRequest) {
        JsonNode reqBody = new ObjectMapper().convertValue(callbackRequest, JsonNode.class);
        return authRequest(sharerUserName)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .post("/sharees");
    }

    protected Response deleteRecordings(List<Long> ccdCaseIds) {
        JsonNode reqBody = new ObjectMapper().convertValue(ccdCaseIds, JsonNode.class);
        return authRequest(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete(DELETE_PATH);
    }

    protected Response deleteRecordingsWithInvalidS2S(List<Long> ccdCaseIds) {
        JsonNode reqBody = new ObjectMapper().convertValue(ccdCaseIds, JsonNode.class);
        return userAuthRequest(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS)
            .header(SERVICE_AUTHORIZATION, "invalid")
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete(DELETE_PATH);
    }

    protected Response deleteRecordingsWithUnauthorisedS2S(List<Long> ccdCaseIds) {
        JsonNode reqBody = new ObjectMapper().convertValue(ccdCaseIds, JsonNode.class);
        return userAuthRequest(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS)
            .header(SERVICE_AUTHORIZATION, extendedCcdHelper.getCcdS2sToken())
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete(DELETE_PATH);
    }

    protected Response downloadRecording(String userName, Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segmentNodes = (ArrayList) caseData.getOrDefault(
            "recordingFiles",
            new ArrayList<String>()
        );

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(CaseRecordingFile::getCaseDocument)
            .map(CaseDocument::getBinaryUrl)
            .findFirst()
            .orElseThrow();

        return authRequest(userName)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get(recordingUrl);
    }

    protected Response downloadShareeRecording(String userName, Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segmentNodes
            = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList<String>());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(CaseRecordingFile::getCaseDocument)
            .map(CaseDocument::getBinaryUrl)
            .findFirst()
            .orElseThrow();

        return authRequest(userName)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get(recordingUrl + "/sharee");
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
            .put("source-blob-url", cvpContainerUrl + filename)
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

    CaseDetails findCaseWithAutoRetryWithUserWithSearcherRole(String caseRef) {
        Optional<CaseDetails> optionalCaseDetails = searchForCaseWithUserWithSearcherRole(caseRef);

        int count = 0;
        while (count <= 3 && optionalCaseDetails.isEmpty()) {
            SleepHelper.sleepForSeconds(FIND_CASE_TIMEOUT);
            LOGGER.info("Search attempt # {}", count);
            optionalCaseDetails = searchForCaseWithUserWithSearcherRole(caseRef);
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

    protected Optional<CaseDetails> searchForCaseWithUserWithSearcherRole(String caseRef) {
        Map<String, String> searchCriteria = Map.of("case.recordingReference", caseRef);
        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(
            USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, DUMMY_USER_DEFAULT_PASS);
        String uid = idamClient.getUserInfo(userToken).getUid();

        LOGGER.info("with Jurisdiction {} and casetype {}", JURISDICTION, CASE_TYPE);
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


    public String closeCaseWithSystemUser(final String caseRef, CaseDetails caseDetails) {

        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(
            SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION, DUMMY_USER_DEFAULT_PASS);
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

        String caseState = caseDetails.getState();
        assert (caseState.equals("1_CLOSED"));
        LOGGER.info("closed case id ({}) with reference ({}), it now has state ({})",
                    caseDetails.getId(), caseRef, caseDetails.getState()
        );
        return caseDetails.getState();
    }


    /**
     * below three methods denerate a random valid UID.
     * ripped from uk.gov.hmcts.ccd.domain.service.common
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
     * @return check digit
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
