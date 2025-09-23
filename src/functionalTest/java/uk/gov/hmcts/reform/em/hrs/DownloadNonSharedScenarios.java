package uk.gov.hmcts.reform.em.hrs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;
import uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class DownloadNonSharedScenarios extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadNonSharedScenarios.class);
    private Set<String> filenames = new HashSet<>();
    private BlobUtil blobUtil;
    private CaseDetails caseDetails;
    private int expectedFileSize;

    @Autowired
    DownloadNonSharedScenarios(
        IdamClient idamClient,
        IdamHelper idamHelper,
        S2sHelper s2sHelper,
        CoreCaseDataApi coreCaseDataApi,
        ExtendedCcdHelper extendedCcdHelper,
        BlobUtil blobUtil
    ) {
        super(idamClient, idamHelper, s2sHelper, coreCaseDataApi, extendedCcdHelper);
        this.blobUtil = blobUtil;
    }

    @BeforeEach
    public void setup() throws Exception {
        if (Objects.nonNull(caseDetails) && Objects.nonNull(caseDetails.getData())) {
            LOGGER.info("CaseDetails is not null, setup done already");
            return;
        }
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        String caseRef = timebasedCaseRef();
        String filename = filename(caseRef, 0);
        filenames.add(filename);

        LOGGER.info("Priming CVP Container");
        blobUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.cvpBlobContainerClient);

        LOGGER.info("Priming HRS API With Posted Segments");
        postRecordingSegment(caseRef, 0).then().statusCode(202);
        blobUtil.checkIfUploadedToStore(filenames, blobUtil.hrsCvpBlobContainerClient);


        LOGGER.info("Checking CCD and populating default caseDetails");
        caseDetails = findCaseWithAutoRetryWithUserWithSearcherRole(caseRef);

        //used in tests to verify file is fully downloaded
        expectedFileSize = blobUtil.getFileFromPath("data/test_data.mp4").readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));
    }

    @Test
    public void userWithCaseWorkerHrsSearcherRoleShouldBeAbleToDownloadHearingRecordings() {
        final byte[] downloadedFileBytes =
            downloadRecording(USER_WITH_SEARCHER_ROLE_CASEWORKER_HRS, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void userWithOnlyCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_REQUESTOR_ROLE_CASEWORKER_ONLY, caseDetails.getData())
            .then()
            .statusCode(403);
    }

    @Test
    public void userWithOnlyCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() {
        downloadRecording(USER_WITH_NONACCESS_ROLE_CITIZEN, caseDetails.getData())
            .then()
            .statusCode(403);
    }
}
