package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.helper.TestClockProvider;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorageImpl;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BlobStoreInspectorControllerTest extends BaseWebTest {

    @MockitoBean
    private HearingRecordingStorage hearingRecordingStorage;

    private String testDummyKey = "RkI2ejoxNjk1OTA2MjM0MDcx";

    @BeforeEach
    public void setup() {
        super.setup();
        TestClockProvider.stoppedInstant = ZonedDateTime.now().toInstant();
    }

    private void stopTime() {
        Instant stopPastTime = ZonedDateTime
            .of(2012, 1, 1, 1, 1, 1, 1, EUROPE_LONDON_ZONE_ID)
            .withHour(5).toInstant();
        TestClockProvider.stoppedInstant = stopPastTime;
    }

    @Test
    public void inspectEndpointReturnsResponse() throws Exception {

        var today = LocalDate.now();
        var storageReport = new StorageReport(
            today,
            new StorageReport.HrsSourceVsDestinationCounts(1567, 1232, 332, 1000)
        );

        stopTime();

        when(hearingRecordingStorage.getStorageReport()).thenReturn(storageReport);
        mockMvc.perform(get("/report")
                            .header(AUTHORIZATION, "Bearer " + testDummyKey))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value(today.toString()))
            .andExpect(jsonPath("$.cvp-item-count").value(1567))
            .andExpect(jsonPath("$.hrs-cvp-item-count").value(1232))
            .andExpect(jsonPath("$.cvp-item-count-today").value(332))
            .andExpect(jsonPath("$.hrs-cvp-item-count-today").value(1000));
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
    public void findBlobEndpointReturnsResponse() throws Exception {
        stopTime();
        String blobName = UUID.randomUUID() + ".txt";
        OffsetDateTime time = OffsetDateTime.now(EUROPE_LONDON_ZONE_ID).truncatedTo(ChronoUnit.SECONDS);

        String blobUrl = "http://cvp.blob/" + blobName;
        when(hearingRecordingStorage.findBlob(HearingSource.CVP, blobName)).thenReturn(
            new HearingRecordingStorageImpl.BlobDetail(blobUrl, 10, time)
        );
        mockMvc.perform(get("/report/hrs/CVP/" + blobName)
                            .header(AUTHORIZATION, "Bearer " + testDummyKey))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.blob-url").value(blobUrl))
            .andExpect(jsonPath("$.blob-size").value(10))
            .andExpect(jsonPath("$.last-modified").value(time + ""));
    }

    @Test
    public void findBlobEndpointReturns401ErrorIfApiKeyInvalid() throws Exception {
        String blobName = UUID.randomUUID() + ".txt";
        mockMvc.perform(get("/report/hrs/CVP/" + blobName)
                            .header(AUTHORIZATION, "Bearer invalid"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
}
