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
import javax.annotation.PostConstruct;

import static uk.gov.hmcts.reform.em.hrs.BaseTest.HRS_TESTER;

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


    @PostConstruct
    public void init() throws Exception {

        importDefinitionFile();
    }

    public String getCcdS2sToken() {
        return ccdAuthTokenGenerator.generate();
    }

    private void importDefinitionFile() throws IOException {

        createUserRole("caseworker");
        createUserRole("caseworker-hrs");

        MultipartFile multipartFile = new MockMultipartFile(
            "x",
            "x",
            "application/octet-stream",
            getHrsDefinitionFile()
        );

        ccdDefImportApi.importCaseDefinition(idamHelper.authenticateUser(HRS_TESTER),
                                             ccdAuthTokenGenerator.generate(), multipartFile
        );
    }

    private InputStream getHrsDefinitionFile() {
        return ClassLoader.getSystemResourceAsStream(ccdDefinitionFile);
    }

    private void createUserRole(String userRole) {
        ccdDefUserRoleApi.createUserRole(new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
                                         idamHelper.authenticateUser(HRS_TESTER), ccdAuthTokenGenerator.generate()
        );
    }
}
