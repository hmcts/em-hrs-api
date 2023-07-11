package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private HearingRecordingStorage hearingRecordingStorage;
    @Mock
    private Snooper snooper;

    @InjectMocks
    private IngestionServiceImpl sutIngestionService;

    @Test
    void testShouldCopyToAzureStorageAndJobToCcdQueueWhenHearingRecordingIsNew() {

        doNothing()
            .when(hearingRecordingStorage)
            .copyRecording(HEARING_RECORDING_DTO.getSourceBlobUrl(), HEARING_RECORDING_DTO.getFilename());

        sutIngestionService.ingest(HEARING_RECORDING_DTO);

        verify(hearingRecordingStorage).copyRecording(
            HEARING_RECORDING_DTO.getSourceBlobUrl(),
            HEARING_RECORDING_DTO.getFilename()
        );

        verifyNoInteractions(snooper);
    }
}
