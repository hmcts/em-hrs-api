package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.hrs.helper.TestClockProvider;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlobStoreInspectorController.class)
@Import(TestClockProvider.class)
@TestPropertySource(properties = {
    "report.api-key=RkI2ejoxNjk1OTA2MjM0MDcx",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BlobStoreInspectorControllerTest extends BaseWebTest {

    @MockBean
    private HearingRecordingStorage hearingRecordingStorage;

    private String testDummyKey = "RkI2ejoxNjk1OTA2MjM0MDcx";

    @BeforeEach
    public void setup() {
        super.setup();
        TestClockProvider.stoppedInstant = ZonedDateTime.now().toInstant();
    }

    @Test
    public void inspectEndpointReturns401ErrorIfApiKeyMissing() throws Exception {
        mockMvc.perform(get("/report"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void inspectEndpointReturns401ErrorIfApiKeyExpired() throws Exception {
        mockMvc.perform(get("/report")
                            .header(AUTHORIZATION, "Bearer " + testDummyKey))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void inspectEndpointReturns401ErrorIfApiKeyInvalid() throws Exception {
        mockMvc.perform(get("/report")
                            .header(AUTHORIZATION, "Bearer invalid"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }


    @Test
    public void findBlobEndpointReturns401ErrorIfApiKeyInvalid() throws Exception {
        String blobName = UUID.randomUUID() + ".txt";
        mockMvc.perform(get("/report/hrs/VH/" + blobName)
                            .header(AUTHORIZATION, "Bearer invalid"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
}
