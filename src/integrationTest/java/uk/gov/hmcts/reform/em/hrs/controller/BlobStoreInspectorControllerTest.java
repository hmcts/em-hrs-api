package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlobStoreInspectorController.class)
public class BlobStoreInspectorControllerTest extends BaseWebTest {

    @MockBean
    HearingRecordingStorage hearingRecordingStorage;


    @Test
    public void inspectEndpoint() throws Exception {

        var today = LocalDate.now();
        var storageReport = new StorageReport(today, 1567, 1232, 332, 232);
        when(hearingRecordingStorage.getStorageReport()).thenReturn(storageReport);
        MvcResult response = mockMvc.perform(get("/inspect")).andExpect(status().isOk()).andReturn();

        mockMvc.perform(get("/inspect/"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value(today.toString()))
            .andExpect(jsonPath("$.cvp-item-count").value(1567))
            .andExpect(jsonPath("$.hrs-item-count").value(1232))
            .andExpect(jsonPath("$.cvp-item-count-today").value(332))
            .andExpect(jsonPath("$.hrs-item-count-today").value(232));
    }
}
