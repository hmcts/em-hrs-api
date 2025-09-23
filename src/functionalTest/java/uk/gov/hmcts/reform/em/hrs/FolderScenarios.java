package uk.gov.hmcts.reform.em.hrs;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class FolderScenarios extends BaseTest {

    @Autowired
    FolderScenarios(
        IdamClient idamClient,
        IdamHelper idamHelper,
        S2sHelper s2sHelper,
        CoreCaseDataApi coreCaseDataApi,
        ExtendedCcdHelper extendedCcdHelper
    ) {
        super(idamClient, idamHelper, s2sHelper, coreCaseDataApi, extendedCcdHelper);
    }

    @Test
    public void shouldCreateFolderWhenDoesNotExistAndReturnEmptyFileNames() {
        final String nonExistentFolder = "audiostream000000";

        getFilenamesCompletedOrInProgress(nonExistentFolder)
            .assertThat()
            .statusCode(200)
            .body("folder-name", equalTo(nonExistentFolder))
            .body("filenames", empty());
    }
}
