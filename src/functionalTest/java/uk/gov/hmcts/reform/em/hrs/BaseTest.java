package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
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
import uk.gov.hmcts.reform.em.test.idam.IdamConfiguration;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;
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
import java.util.UUID;

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
@RunWith(SpringJUnit4ClassRunner.class)
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
    protected static final String INTERPRETER = "interpreter12";


    public static String SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION =
        "hrs.functional.system.user@hmcts.net";

    protected static final String USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS = "em-test-searcher@test.hmcts.net";
    protected static final String USER_WITH_REQUESTOR_ROLE__CASEWORKER_ONLY = "em-test-requestor@test.hmcts.net";
    protected static final String USER_WITH_NONACCESS_ROLE__CITIZEN = "em-test-citizen@test.hmcts.net";
    protected static final String USER_DEFAULT_PASSWORD = "4590fgvhbfgbDdffm3lk4j";//USED ONLY FOR TESTS in IDAM HELPER
    protected static final String EMAIL_ADDRESS_INVALID_FORMAT = "invalid@emailaddress";

    protected static final String FOLDER =
        "audiostream" + LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd"));
    protected static final String TIME = "2020-11-04-14.56.32.819";
    public static final String CASEREF_PREFIX = "FUNCTEST_";

    public static final String VH_CASEREF_PREFIX = "VH-FUNCT-";
    protected static final String CLOSE_CASE = "closeCase";

    protected static int FIND_CASE_TIMEOUT = 30;

    protected String hrsS2sAuth;

    @Value("${close-ccd-test-cases}")
    protected boolean closeCcdCase;

    //The format "yyyy-MM-dd---HH-MM-ss---SSS" will render "07-30-2021---16-07-35---485"
    DateTimeFormatter datePartFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dateTimePartFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHMMssSSS");
    DateTimeFormatter timePartFormatter = DateTimeFormatter.ofPattern("HH-MM-ss---SSS");

    @Rule
    public RetryRule retryRule = new RetryRule(3);//3 is standard across hmcts projects

    @Value("${test.url}")
    protected String testUrl;

    @Value("${azure.storage.cvp.container-url}")
    private String cvpContainerUrl;

    @Value("${azure.storage.vh.container-url}")
    private String vhContainerUrl;

    @Value("${idam.hrs-ingestor.user-name}")
    private String idamHrsIngestorUserName;

    @Value("${idam.hrs-ingestor.password}")
    private String idamHrsIngestorPassword;

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
        LOGGER.info("AUTHENTICATING TEST USER FOR CCD CALLS");
        hrsS2sAuth = BEARER + s2sHelper.getS2sToken();
    }

    private void createIdamUserIfNotExists(String email, List<String> roles) {
        /*

        if multiple PR branches are triggered, then it means the user token cache used by em-test-helper
        will become stale

        potential for conflict with hrs-api using the hrs.tester@hmcts.net system user
        probably these tests should not use that user, however many issues arose when
        trying to refactor this logic and there was not enough time to see it through.

        good to have set to true for local environments, when testing role changes etc

        TODO make recreateUsers an environment value so that it is true for local dev, and false for AAT
         */
        boolean recreateUsers = true;

        if (recreateUsers) {
            LOGGER.info("CREATING USER {} with roles {}", email, roles);
            idamHelper.createUser(email, roles);
        } else {
            try {
                String userId = idamHelper.getUserId(email);
                LOGGER.info("User {} already exists: id={}", email, userId);
            } catch (Exception e) {
                //if user does not exist
                LOGGER.info(
                    "Exception thrown, likely user does not exist so will create. Ignore the above Exception:{}",
                    e.getMessage()
                );
                LOGGER.info("CREATING USER {} with roles {}", email, roles);
                idamHelper.createUser(email, roles);
            }
        }

    }


    public RequestSpecification authRequestForSearcherRole() {
        return authRequest(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS);
    }


    public RequestSpecification authRequestForHrsIngestor() {
        LOGGER.info("authRequestForHrsIngestor {}, {}", this.idamHrsIngestorUserName, this.idamHrsIngestorPassword);
        return authRequest(this.idamHrsIngestorUserName, this.idamHrsIngestorPassword);
    }


    private RequestSpecification authRequest(String username, String password) {
        return setJwtTokenHeader(idamHelper.authenticateUser(username, password))
            .header("ServiceAuthorization", hrsS2sAuth);
    }

    private RequestSpecification authRequest(String username) {
        LOGGER.info("authRequestForUsername username {}", username);
        return setJwtTokenHeader(idamHelper.authenticateUser(username))
            .header("ServiceAuthorization", hrsS2sAuth);
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
            .header("ServiceAuthorization", hrsS2sAuth);
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

    protected Response postVhRecordingSegment(String caseRef, int segment, UUID hearingRef, String fileName) {
        final JsonNode segmentPayload = createVhSegmentPayload(caseRef, segment, hearingRef, fileName);
        return postRecordingSegment(segmentPayload);
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
        return authRequest(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete("/delete");
    }

    protected Response deleteRecordingsWithInvalidS2S(List<Long> ccdCaseIds) {
        JsonNode reqBody = new ObjectMapper().convertValue(ccdCaseIds, JsonNode.class);
        return userAuthRequest(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS)
            .header("ServiceAuthorization", "invalid")
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete("/delete");
    }

    protected Response deleteRecordingsWithUnauthorisedS2S(List<Long> ccdCaseIds) {
        JsonNode reqBody = new ObjectMapper().convertValue(ccdCaseIds, JsonNode.class);
        return userAuthRequest(USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS)
            .header("ServiceAuthorization", extendedCcdHelper.getCcdS2sToken())
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(reqBody)
            .when().log().all()
            .delete("/delete");
    }

    protected Response downloadRecording(String userName, Map<String, Object> caseData) {
        @SuppressWarnings("unchecked")
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(caseRecordingFile -> caseRecordingFile.getCaseDocument())
            .map(caseDocument -> caseDocument.getBinaryUrl())
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
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        String recordingUrl = segmentNodes.stream()
            .map(segmentNode -> new ObjectMapper().convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .map(caseRecordingFile -> caseRecordingFile.getCaseDocument())
            .map(caseDocument -> caseDocument.getBinaryUrl())
            .findFirst()
            .orElseThrow();

        return authRequest(userName)
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .when().log().all()
            .get(recordingUrl + "/sharee");
    }

    protected JsonNode createVhSegmentPayload(String caseRef, int segment, UUID hearingRef, String fileName) {
        return createVhRecordingSegment(
            JURISDICTION,
            LOCATION_CODE,
            caseRef,
            TIME,
            segment,
            FILE_EXT,
            fileName,
            hearingRef.toString()
        );
    }

    protected JsonNode createVhRecordingSegment(
        String jurisdictionCode,
        String locationCode,
        String caseRef,
        String recordingTime,
        int segment,
        String fileExt,
        String fileName,
        String recordingRef
    ) {

        return JsonNodeFactory.instance.objectNode()
            .put("folder", "VH")
            .put("recording-ref", recordingRef)
            .put("recording-source", "VH")
            .put("court-location-code", locationCode)
            .put("service-code", "PROBATE")
            .put("hearing-room-ref", "London")
            .put("jurisdiction-code", jurisdictionCode)
            .put("case-ref", caseRef)
            .put("source-blob-url", vhContainerUrl + fileName)
            .put("filename", fileName)
            .put("filename-extension", fileExt)
            .put("file-size", 200724364L)
            .put("segment", segment)
            .put("recording-date-time", recordingTime);
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
            USER_WITH_SEARCHER_ROLE__CASEWORKER_HRS, USER_DEFAULT_PASSWORD);
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

    protected String timeVhBasedCaseRef() {

        ZonedDateTime now = ZonedDateTime.now();

        String dateTime = dateTimePartFormatter.format(now);
        //yyyy-MM-dd---HH-MM-ss---SSS=07-30-2021---16-07-35---485
        return VH_CASEREF_PREFIX + dateTime;
    }

    protected String filename(String caseRef, int segment) {
        return FOLDER + "/" + JURISDICTION + "-" + LOCATION_CODE + "-" + caseRef + "_" + TIME
            + "-UTC_" + segment + ".mp4";
    }

    protected String vhFileName(String caseRef, int segment, String interpreter, UUID hearingRef) {
        if (interpreter != null && interpreter.toLowerCase().startsWith("interpreter")) {
            interpreter = interpreter + "_";
        } else {
            interpreter = "";
        }
        return JURISDICTION + "-" + caseRef + "-" + hearingRef + "_" + interpreter + TIME
            + "-UTC_" + segment + ".mp4";
    }

    public String closeCaseWithSystemUser(final String caseRef, CaseDetails caseDetails) {

        String s2sToken = extendedCcdHelper.getCcdS2sToken();
        String userToken = idamClient.getAccessToken(
            SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION, USER_DEFAULT_PASSWORD);
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
