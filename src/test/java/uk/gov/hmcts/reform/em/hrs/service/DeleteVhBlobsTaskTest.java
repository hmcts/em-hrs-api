package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class DeleteVhBlobsTaskTest {

    @Mock
    private HearingRecordingStorage hearingRecordingStorage;

    @InjectMocks
    private DeleteVhBlobsTask deleteVhBlobsTask;

    private static final String TASK_NAME = "delete-vh-blob";
    private static final Logger logger = getLogger(DeleteVhBlobsTask.class);

    @Test
    void run_ShouldListVHBlobsAndLog_WhenCalled() {
        // Call the run() method
        deleteVhBlobsTask.run();

        // Verify that listVHBlobs() method is called once
        verify(hearingRecordingStorage).listVHBlobs();
    }
}
