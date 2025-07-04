package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.doNothing;

@Provider("em_hrs_api_recording_sharees_provider")
public class HearingRecordingShareesProviderTest extends HearingControllerBaseProviderTest {
    @State("Permission record can be created for a hearing recording and user notified")
    public void setupShareAndNotify() {
        // Mock the shareAndNotifyService to do nothing on shareAndNotify
        doNothing().when(shareAndNotifyService).shareAndNotify(
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.anyString()
        );
    }
}
