package uk.gov.hmcts.reform.em.hrs.testutil;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefImportApi;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefUserRoleApi;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.em.hrs.BaseTest.SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION;

@Service
public class ExtendedCcdHelper {

    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final String CLOSE_EVENT_TYPE_ID = "closeCase";
    private static final String CLOSE_EVENT_SUMMARY = "Create by HRS api Functional Tests,Closed by HRS api";

    @Autowired
    private IdamHelper idamHelper;

    @Qualifier("ccdAuthTokenGenerator")
    @Autowired
    private AuthTokenGenerator ccdAuthTokenGenerator;

    @Autowired
    private CcdDefImportApi ccdDefImportApi;

    @Autowired
    private CcdDefUserRoleApi ccdDefUserRoleApi;

    @Value("${ccd-def.file}")
    protected String ccdDefinitionFile;


    @Value("${core_case_data.api.url}")
    protected String ccdApiUrl;


    public String getCcdS2sToken() {
        return ccdAuthTokenGenerator.generate();
    }

    public void importDefinitionFile() throws IOException {

        //These roles need to exist in both IDAM and CCD
        //Their counterparts are created in idam as part of docker/dependencies/start-local-environment.sh
        createCcdUserRole("caseworker");
        createCcdUserRole("caseworker-hrs");//required as is 'parent' of caseworker-hrs-searcher
        createCcdUserRole("caseworker-hrs-searcher");

        MultipartFile ccdDefinitionRequest = new MockMultipartFile(
            "x",
            "x",
            "application/octet-stream",
            getHrsDefinitionFile()
        );

        String systemUserAuthenticatedToken = idamHelper.authenticateUser(
            SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION);
        String microserviceEmHrsApiAuthenticatedToken = ccdAuthTokenGenerator.generate();
        ccdDefImportApi.importCaseDefinition(systemUserAuthenticatedToken,
                                             microserviceEmHrsApiAuthenticatedToken, ccdDefinitionRequest
        );
    }

    private InputStream getHrsDefinitionFile() {
        return ClassLoader.getSystemResourceAsStream(ccdDefinitionFile);
    }

    private void createCcdUserRole(String userRole) {

        var valur = ccdAuthTokenGenerator.generate();
        System.out.println("ccd au.generate() ===> " + valur);
        ccdDefUserRoleApi.createUserRole(
            new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
            idamHelper.authenticateUser(SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION),
            valur
        );
    }

    public void closeCcdCase(Long caseId) {

        String userId = idamHelper.getUserId("hrs.tester@hmcts.net");

        String idamToken = idamHelper.authenticateUser(
            "hrs.tester@hmcts.net");
        String s2sToken = ccdAuthTokenGenerator.generate();

        String caseTypeId = "HearingRecordings";

        StartEventResponse startEventResponse =
            startEvent(
                idamToken,
                s2sToken,
                userId,
                "HRS",
                caseTypeId,
                caseId,
                CLOSE_EVENT_TYPE_ID
            );


        Map<String, Object> caseData = startEventResponse.getCaseDetails().getData();

        CaseDataContent newCaseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder()
                       .id(CLOSE_EVENT_TYPE_ID)
                       .summary(CLOSE_EVENT_SUMMARY)
                       .build())
            .data(caseData)
            .build();

        submitEvent(
            idamToken,
            s2sToken,
            userId,
            "HRS",
            caseTypeId,
            caseId,
            newCaseDataContent
        );
    }


    private StartEventResponse startEvent(
        String idamToken,
        String s2sToken,
        String userId,
        String jurisdictionId,
        String caseType,
        Long caseId,
        String eventId
    ) {
        return getRequestSpecification(idamToken, s2sToken).log().all()
            .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
            .pathParam("userId", userId)
            .pathParam("jurisdictionId", jurisdictionId)
            .pathParam("caseType", caseType)
            .pathParam("caseId", caseId)
            .pathParam("eventId", eventId)
            .get(
                "/caseworkers/{userId}"
                    + "/jurisdictions/{jurisdictionId}"
                    + "/case-types/{caseType}"
                    + "/cases/{caseId}"
                    + "/event-triggers/{eventId}/token"
            )
            .then().log().all()
            .assertThat()
            .statusCode(SC_OK)
            .extract()
            .as(StartEventResponse.class);
    }

    private CaseDetails submitEvent(
        String idamToken,
        String s2sToken,
        String userId,
        String jurisdictionId,
        String caseType,
        Long caseId,
        CaseDataContent caseDataContent
    ) {
        return getRequestSpecification(idamToken, s2sToken)
            .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType()).log().all()
            .pathParam("userId", userId)
            .pathParam("jurisdictionId", jurisdictionId)
            .pathParam("caseType", caseType)
            .pathParam("caseId", caseId)
            .body(caseDataContent)
            .post(
                "/caseworkers/{userId}"
                    + "/jurisdictions/{jurisdictionId}"
                    + "/case-types/{caseType}"
                    + "/cases/{caseId}"
                    + "/events?ignoreWarning=true"
            )
            .then().log().all()
            .assertThat()
            .statusCode(SC_CREATED)
            .extract()
            .as(CaseDetails.class);
    }

    private RequestSpecification getRequestSpecification(String idamToken, String s2sToken) {

        if (!StringUtils.startsWith(idamToken, BEARER_TOKEN_PREFIX)) {
            idamToken = BEARER_TOKEN_PREFIX + idamToken;
        }
        if (!StringUtils.startsWith(s2sToken, BEARER_TOKEN_PREFIX)) {
            s2sToken = BEARER_TOKEN_PREFIX + s2sToken;
        }
        return RestAssured
            .given().log().all()
            .relaxedHTTPSValidation()
            .baseUri(ccdApiUrl)
            .header("experimental", true)
            .header("Authorization", idamToken)
            .header("ServiceAuthorization", s2sToken);
    }
}
