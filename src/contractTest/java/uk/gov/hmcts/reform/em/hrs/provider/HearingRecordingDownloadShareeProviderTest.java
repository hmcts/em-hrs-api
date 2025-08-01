package uk.gov.hmcts.reform.em.hrs.provider;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.em.hrs.testconfig.MockSegmentDownloadConfig;

@Provider("em_hrs_api_recording_download_sharee_provider")
@Import(MockSegmentDownloadConfig.class)
public class HearingRecordingDownloadShareeProviderTest extends HearingControllerBaseProviderTest {


    @State({"A segment exists for sharee to download by recording ID and segment number",
        "A segment exists for sharee to download by recording ID and file name",
        "A segment exists for sharee to download by recording ID and folder and file name"
    })
    public void setupValidSegmentDownload() {

    }
}
