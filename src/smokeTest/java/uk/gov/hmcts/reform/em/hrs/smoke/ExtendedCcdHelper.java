package uk.gov.hmcts.reform.em.hrs.smoke;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefImportApi;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefUserRoleApi;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.io.IOException;
import java.io.InputStream;

import static uk.gov.hmcts.reform.em.hrs.smoke.SmokeTest.SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION;

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
        var serviceToken = ccdAuthTokenGenerator.generate();
        var idamToken = idamHelper.authenticateUser(SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION);
        //These roles need to exist in both IDAM and CCD
        //Their counterparts are created in idam as part of docker/dependencies/start-local-environment.sh
        createCcdUserRole("caseworker", serviceToken, idamToken);
        createCcdUserRole(
            "caseworker-hrs",
            serviceToken,
            idamToken
        );//required as is 'parent' of caseworker-hrs-searcher
        createCcdUserRole("caseworker-hrs-searcher", serviceToken, idamToken);
        createCcdUserRole("cft-ttl-manager", serviceToken, idamToken);
        createCcdUserRole("caseworker-hrs-systemupdate", serviceToken, idamToken);

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

    private void createCcdUserRole(String userRole, String serviceToken, String idamToken) {
        int maxAttempts = 3;
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                ccdDefUserRoleApi.createUserRole(
                    new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
                    idamToken,
                    serviceToken
                );
                System.out.println("userRole created===> " + userRole);
                break; // Success, exit loop
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    System.err.println("Failed to create userRole after " + maxAttempts + " attempts");
                    throw e; // Rethrow after final attempt
                }
                System.err.println("Attempt " + attempt + " failed, retrying in 2 seconds...");
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
}
