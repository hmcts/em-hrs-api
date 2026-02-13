package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private HearingRecordingStorage hearingRecordingStorage;

    @Mock
    private HearingRecordingDto hearingRecordingDto;

    @InjectMocks
    private IngestionServiceImpl sutIngestionService;

    @Test
    void testShouldCopyToAzureStorageAndJobToCcdQueueWhenHearingRecordingIsNew() {
        sutIngestionService.ingest(hearingRecordingDto);

        verify(hearingRecordingStorage).copyRecording(hearingRecordingDto);
    }
}
