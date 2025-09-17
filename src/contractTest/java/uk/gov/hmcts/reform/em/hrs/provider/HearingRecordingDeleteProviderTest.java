package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;

import java.util.List;

import static org.mockito.Mockito.doNothing;

@Provider("em_hrs_api_recording_delete_provider")
public class HearingRecordingDeleteProviderTest extends HearingControllerBaseProviderTest {

    @MockitoBean
    private SegmentDownloadService segmentDownloadService;

    public HearingRecordingDeleteProviderTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @State("Hearing recordings exist for given CCD case IDs to delete")
    public void setupHearingRecordingsExist() {
        // Mock the service to do nothing on delete
        doNothing().when(hearingRecordingService).deleteCaseHearingRecordings(List.of(123456789L, 987654321L));
    }
}
