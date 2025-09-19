package uk.gov.hmcts.reform.em.hrs.smoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private IdamHelper idamHelper;
    private AuthTokenGenerator ccdAuthTokenGenerator;
    private CcdDefImportApi ccdDefImportApi;
    private CcdDefUserRoleApi ccdDefUserRoleApi;

    @Value("${ccd-def.file}")
    protected String ccdDefinitionFile;
    @Value("${core_case_data.api.url}")
    protected String ccdApiUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedCcdHelper.class);

    public ExtendedCcdHelper(IdamHelper idamHelper,
                             @Qualifier("ccdAuthTokenGenerator")AuthTokenGenerator ccdAuthTokenGenerator,
                             CcdDefImportApi ccdDefImportApi,
                             CcdDefUserRoleApi ccdDefUserRoleApi
    ) {
        this.idamHelper = idamHelper;
        this.ccdAuthTokenGenerator = ccdAuthTokenGenerator;
        this.ccdDefImportApi = ccdDefImportApi;
        this.ccdDefUserRoleApi = ccdDefUserRoleApi;
    }

    public String getCcdS2sToken() {
        return ccdAuthTokenGenerator.generate();
    }

    public void importDefinitionFile() throws IOException, InterruptedException {
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

    private void createCcdUserRole(String userRole, String serviceToken, String idamToken)
        throws InterruptedException {
        int maxAttempts = 3;
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                ccdDefUserRoleApi.createUserRole(
                    new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
                    idamToken,
                    serviceToken
                );
                break; // Success, exit loop
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    LOGGER.error("Failed to create userRole after {} attempts", maxAttempts);
                    throw e; // Rethrow after final attempt
                }
                LOGGER.error("Attempt {} failed, retrying in 2 seconds...", attempt);
                Thread.sleep(2000); // Wait 2 seconds before retrying
            }
        }
    }
}
