package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.em.hrs.componenttests.AbstractBaseTest;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {BlobStoreInspectorController.class})
public class BlobstoreInspectorTest extends AbstractBaseTest {

    @MockBean
    HearingRecordingStorage hearingRecordingStorage;


    @Test
    public void inspectEndpoint() throws Exception {

        var today = LocalDate.now();
        var storageReport = new StorageReport(today, 12, 21, 2, 6);
        when(hearingRecordingStorage.getStorageReport()).thenReturn(storageReport);
        MvcResult response = mockMvc.perform(get("/inspect")).andExpect(status().isOk()).andReturn();
        assertThat(response.getResponse().getContentAsString()).startsWith("Blobstores");
    }
}
