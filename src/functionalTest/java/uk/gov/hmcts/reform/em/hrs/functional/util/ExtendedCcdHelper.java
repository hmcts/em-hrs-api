package uk.gov.hmcts.reform.em.hrs.functional.util;

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
import java.util.List;
import javax.annotation.PostConstruct;

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


    public static String HRS_TESTER = "hrs.test.user@hmcts.net";
    public static List<String> HRS_TESTER_ROLES = List.of("caseworker", "caseworker-hrs", "ccd-import");

    @PostConstruct
    public void init() throws Exception {
        idamHelper.createUser(HRS_TESTER, HRS_TESTER_ROLES);
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
            getHrsDefinitionFile());

        ccdDefImportApi.importCaseDefinition(idamHelper.authenticateUser(HRS_TESTER),
                                             ccdAuthTokenGenerator.generate(), multipartFile);
    }

    private InputStream getHrsDefinitionFile() {
        return ClassLoader.getSystemResourceAsStream(ccdDefinitionFile);
    }

    private void createUserRole(String userRole) {
        ccdDefUserRoleApi.createUserRole(new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
                                         idamHelper.authenticateUser(HRS_TESTER), ccdAuthTokenGenerator.generate());
    }
}
