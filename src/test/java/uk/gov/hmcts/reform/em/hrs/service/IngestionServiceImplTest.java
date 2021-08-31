package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private HearingRecordingStorage hearingRecordingStorage;
    @Mock
    private Snooper snooper;

    @Mock
    LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue;

    @InjectMocks
    private IngestionServiceImpl underTest;

    @Test
    void testShouldCopyToAzureStorageAndJobToCCDQueueWhenHearingRecordingIsNew() {

        doNothing().when(hearingRecordingStorage)
            .copyRecording(
                HEARING_RECORDING_DTO.getCvpFileUrl(),
                HEARING_RECORDING_DTO.getFilename()
            );

        doReturn(false).when(ccdUploadQueue).offer(HEARING_RECORDING_DTO);

        underTest.ingest(HEARING_RECORDING_DTO);

        verify(hearingRecordingStorage).copyRecording(
            HEARING_RECORDING_DTO.getCvpFileUrl(),
            HEARING_RECORDING_DTO.getFilename()
        );
        verify(ccdUploadQueue).offer(HEARING_RECORDING_DTO);
        verifyNoInteractions(snooper);
    }
}
