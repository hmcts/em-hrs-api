package uk.gov.hmcts.reform.em.hrs;

import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class FolderScenarios extends BaseTest {

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
