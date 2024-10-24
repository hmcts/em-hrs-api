package uk.gov.hmcts.reform.em.hrs;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class FolderScenarios extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderScenarios.class);

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
