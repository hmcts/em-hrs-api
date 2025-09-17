package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.em.hrs.testconfig.MockSegmentDownloadConfig;

@Provider("em_hrs_api_segment_download_provider")
@Import(MockSegmentDownloadConfig.class)
public class HearingRecordingSegmentDownloadProviderTest extends HearingControllerBaseProviderTest {


    public HearingRecordingSegmentDownloadProviderTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @State({"A segment exists to download by recording ID and segment number",
        "A segment exists to download by recording ID and file name",
        "A segment exists to download by recording ID and folder and file name"
    })
    public void setupValidSegmentDownload() {
        // use state for provider test
    }
}
