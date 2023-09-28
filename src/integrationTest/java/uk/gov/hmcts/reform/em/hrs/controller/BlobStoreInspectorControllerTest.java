package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.validation.ClockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.hrs.helper.TestClockProvider;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.hrs.config.ClockConfig.EUROPE_LONDON_ZONE_ID;

@WebMvcTest(BlobStoreInspectorController.class)
@Import(TestClockProvider.class)
@TestPropertySource(properties = {
    "report.api-key=RkI2ejoxNjk1OTA2MjM0MDcx",
})
public class BlobStoreInspectorControllerTest extends BaseWebTest {

    @MockBean
    private HearingRecordingStorage hearingRecordingStorage;

    private ClockProvider clockProvider;

    private String testDummyKey = "RkI2ejoxNjk1OTA2MjM0MDcx";

    @Test
    public void inspectEndpointReturnsResponse() throws Exception {

        var today = LocalDate.now();
        var storageReport = new StorageReport(today, 1567, 1232, 332, 232);

        Instant stopPastTime = ZonedDateTime
            .of(2012, 1, 1, 1, 1, 1, 1, EUROPE_LONDON_ZONE_ID)
            .withHour(5).toInstant();
        givenTheRequestWasMadeAt(stopPastTime);

        when(hearingRecordingStorage.getStorageReport()).thenReturn(storageReport);
        mockMvc.perform(get("/report")
                            .header(AUTHORIZATION, "Bearer " + testDummyKey))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value(today.toString()))
            .andExpect(jsonPath("$.cvp-item-count").value(1567))
            .andExpect(jsonPath("$.hrs-item-count").value(1232))
            .andExpect(jsonPath("$.cvp-item-count-today").value(332))
            .andExpect(jsonPath("$.hrs-item-count-today").value(232));
    }

    @Test
    public void inspectEndpointReturns401ErrorIfApiKeyMissing() throws Exception {
        mockMvc.perform(get("/report")
                            .header(AUTHORIZATION, "Bearer " + testDummyKey))
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

    private void givenTheRequestWasMadeAt(Instant time) {
        TestClockProvider.stoppedInstant = time;
    }
}
