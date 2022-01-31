package uk.gov.hmcts.reform.em.hrs.testutil;

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

import static uk.gov.hmcts.reform.em.hrs.BaseTest.SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION;

@Service
public class ExtendedCcdHelper {

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
        ccdDefUserRoleApi.createUserRole(
            new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
            idamHelper.authenticateUser(SYSTEM_USER_FOR_FUNCTIONAL_TEST_ORCHESTRATION),
            ccdAuthTokenGenerator.generate()
        );
    }
}
