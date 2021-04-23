package uk.gov.hmcts.reform.em.hrs.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefImportApi;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefUserRoleApi;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.annotation.PostConstruct;

@Service
public class ExtendedCcdHelper {

    @Autowired
    private IdamHelper idamHelper;

    @Qualifier("ccdAuthTokenGenerator")
    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private CcdDefImportApi ccdDefImportApi;

    @Autowired
    private CcdDefUserRoleApi ccdDefUserRoleApi;

    private String hrsTester = "hrs.test.user@hmcts.net";
    private List<String> hrTesterRoles = Arrays.asList("caseworker", "caseworker-hrs", "ccd-import");

    @PostConstruct
    public void init() throws Exception {
        initHrsTestUser();
        importDefinitionFile();
    }

    public JsonNode createRecordingSegment(String folder, String url, String filename, String fileExt, Long fileSize, int segment) {
        return JsonNodeFactory.instance.objectNode()
            .put("folder", folder)
            .put("recording-ref", filename)
            .put("recording-source","CVP")
            .put("court-location-code","London")
            .put("service-code","PROBATE")
            .put("hearing-room-ref","12")
            .put("jurisdiction-code","HRS")
            .put("case-ref","hearing-12-family-probate-morning")
            .put("cvp-file-url", url)
            .put("filename", filename)
            .put("filename-extension", fileExt)
            .put("file-size", fileSize)
            .put("segment", segment)
            .put("recording-date-time",
                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS")));
    }

    private void importDefinitionFile() throws IOException {

        createUserRole("caseworker");
        createUserRole("caseworker-hrs");

        MultipartFile multipartFile = new MockMultipartFile(
            "x",
            "x",
            "application/octet-stream",
            getHrsDefinitionFile());

        ccdDefImportApi.importCaseDefinition(idamHelper.authenticateUser(hrsTester),
                                             authTokenGenerator.generate(), multipartFile);

    }

    public void initHrsTestUser() {
        idamHelper.createUser(hrsTester, hrTesterRoles);
    }

    private InputStream getHrsDefinitionFile() {
        return ClassLoader.getSystemResourceAsStream("CCD_CVP_v.03.xlsx");
    }

    private void createUserRole(String userRole) {
        ccdDefUserRoleApi.createUserRole(new CcdDefUserRoleApi.CreateUserRoleBody(userRole, "PUBLIC"),
                                         idamHelper.authenticateUser(hrsTester), authTokenGenerator.generate());
    }

    public Map<String, String> getTokens() {
        return Map.of("user", idamHelper.authenticateUser(hrsTester),
                      "userId", idamHelper.getUserId(hrsTester),
                      "service", authTokenGenerator.generate());
    }

    public JsonNode getShareRequest(String email) {

        ObjectNode caseDocument = JsonNodeFactory.instance.objectNode()
            .put("document_filename", "document_filename")
            .put("document_binary_url", "http://localhost:8080/hearing-recordings/6ac8dc37-d45d-4537-b6ac-149881c85041/segments/0")
            .put("document_url", "http://localhost:8080/hearing-recordings/6ac8dc37-d45d-4537-b6ac-149881c85041/segments/0");

        ObjectNode caseRecordingFile = JsonNodeFactory.instance.objectNode()
            .put("fileSize", 123L)
            .put("segmentNumber", 0)
            .set("documentLink", caseDocument);

        ArrayNode segments = JsonNodeFactory.instance.arrayNode()
            .add(JsonNodeFactory.instance.objectNode().set("value", caseRecordingFile));
        ObjectNode request = JsonNodeFactory.instance.objectNode().set("recordingFiles", segments);
        request.put("recipientEmailAddress", email);
        request.set("recordingFiles", segments);
        return request;
    }

    public String generateToken(){
        return authTokenGenerator.generate();
    }
}
