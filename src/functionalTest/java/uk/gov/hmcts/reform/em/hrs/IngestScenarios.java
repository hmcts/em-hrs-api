package uk.gov.hmcts.reform.em.hrs;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.BlobUtil;
import uk.gov.hmcts.reform.em.hrs.testutil.SleepHelper;

import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IngestScenarios extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestScenarios.class);

    public static final int SEGMENT_COUNT = 1;//TODO set this 10 11 to test CCD validation changes
    public static final int CCD_UPLOAD_WAIT_PER_SEGMENT_IN_SECONDS = 15;
    public static final int CCD_UPLOAD_WAIT_MARGIN_IN_SECONDS = 35;
    //AAT averages at 8/second if evenly spread across servers - 30 seconds if they all were served by 1 server
    //chosing 15 seconds as with 2 segments + 35 second margin = 65 seconds in total 0 more than enough

    @Value("${ttl.enabled}")
    protected boolean ttlEnabled;

    @Value("${ttl.default-ttl}")
    private Period defaultTTL;

    @Autowired
    private BlobUtil testUtil;

    @BeforeEach
    public void setup() {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
    }


    @Test
    public void shouldCreateHearingRecordingSegments() throws Exception {
        String caseRef = timebasedCaseRef();
        Set<String> filenames = new HashSet<>();

        for (int segmentIndex = 0; segmentIndex < SEGMENT_COUNT; segmentIndex++) {
            String filename = filename(caseRef, segmentIndex);
            filenames.add(filename);
            testUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");
            postRecordingSegment(caseRef, segmentIndex)
                .then()
                .log().all()
                .statusCode(202);
        }

        LOGGER.info("************* CHECKING CVP HAS UPLOADED **********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.cvpBlobContainerClient);
        LOGGER.info("************* Files loaded to cvp storage **********");

        LOGGER.info("************* CHECKING HRS HAS COPIED TO STORE **********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.hrsCvpBlobContainerClient);
        assertHearingCcdUpload(filenames, caseRef, FOLDER, SEGMENT_COUNT);
    }

    @Test
    public void shouldCreateHearingRecordingSegmentForVh() throws Exception {
        String caseRef = timeVhBasedCaseRef();
        UUID hearingRef = UUID.randomUUID();
        String filename = vhFileName(caseRef, 0, INTERPRETER, hearingRef);
        testUtil.uploadFileFromPathToVhContainer(filename, "data/test_data.mp4");

        Set<String> filenames = Set.of(filename);
        LOGGER.info("************* CHECKING VH HAS UPLOADED **********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.vhBlobContainerClient);
        LOGGER.info("************* Files loaded to vh storage **********");

        postVhRecordingSegment(
            caseRef,
            0,
            hearingRef,
            filename
        ).then().log().all().statusCode(202);

        LOGGER.info("*********** CHECKING HRS HAS COPIED TO STORE VH container *********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.hrsVhBlobContainerClient);

        long vhFileSize = testUtil.getFileSizeFromStore(filenames, testUtil.vhBlobContainerClient);
        long hrsFileSize = testUtil.getFileSizeFromStore(filenames, testUtil.hrsVhBlobContainerClient);
        assertEquals(hrsFileSize, vhFileSize);

        assertHearingCcdUpload(filenames, caseRef, "VH", 1);
    }

    @Test
    public void shouldCreateHearingRecordingMultipleSegmentsForVh() throws Exception {
        String caseRef = timeVhBasedCaseRef();
        Set<String> filenames;
        List<String> filenameList = new ArrayList<String>();
        UUID hearingRef = UUID.randomUUID();
        int segmentCount = 2;
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            if (segmentIndex == 1) {
                SleepHelper.sleepForSeconds(20);
            }
            String filename = vhFileName(caseRef, segmentIndex, INTERPRETER, hearingRef);
            filenameList.add(filename);
            testUtil.uploadFileFromPathToVhContainer(filename,"data/test_data.mp4");
            // start ingestion
            postVhRecordingSegment(
                caseRef,
                segmentIndex,
                hearingRef,
                filenameList.get(segmentIndex)
            ).then().log().all().statusCode(202);
        }
        filenames = filenameList.stream().collect(Collectors.toSet());

        LOGGER.info("************* CHECKING VH HAS UPLOADED **********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.vhBlobContainerClient);
        LOGGER.info("************* Files loaded to vh storage **********");

        LOGGER.info("*********** CHECKING HRS HAS COPIED TO STORE VH container *********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.hrsVhBlobContainerClient);
        LOGGER.info("************* Files loaded to HRS storage **********");

        assertHearingCcdUpload(filenames, caseRef, "VH", segmentCount);
    }

    @Test
    public void shouldIngestPartiallyCopiedHearingRecordingSegments() throws Exception {
        //Partially copied *should* result in a file size of 0 bytes
        //TODO put link to MS doco describing this
        String caseRef = timebasedCaseRef();

        String filename = filename(caseRef, 0);

        //Upload corrupt file to hrs
        testUtil.uploadFileFromPathToHrsContainer(filename, "data/empty_file.mp4");

        //Upload a real file to cvp
        testUtil.uploadFileFromPathToCvpContainer(filename,"data/test_data.mp4");

        //Sleep for 10 seconds to see the empty file in the Azure Blob Storage - HRS container
        SleepHelper.sleepForSeconds(10);

        LOGGER.info("************* CHECKING CVP HAS UPLOADED **********");
        Set<String> filenames = new HashSet<>();
        testUtil.checkIfUploadedToStore(filenames, testUtil.cvpBlobContainerClient);
        LOGGER.info("************* Files loaded to cvp storage **********");

        //Wait until the ingestion has triggered the copy
        postRecordingSegment(caseRef, 0)
            .then()
            .log().all()
            .statusCode(202);

        SleepHelper.sleepForSeconds(10);

        LOGGER.info("************* CHECKING HRS HAS COPIED TO STORE **********");
        testUtil.checkIfUploadedToStore(filenames, testUtil.hrsCvpBlobContainerClient);

        long cvpFileSize = testUtil.getFileSizeFromStore(filename, testUtil.cvpBlobContainerClient);
        long hrsFileSize = testUtil.getFileSizeFromStore(filename, testUtil.hrsCvpBlobContainerClient);
        assertEquals(hrsFileSize, cvpFileSize);

        assertHearingCcdUpload(filenames, caseRef, FOLDER, 1);

    }

    private void assertHearingCcdUpload(Set<String> filenames, String caseRef, String folder, int segmentCount) {

        LOGGER.info("************* CHECKING HRS HAS IT IN DATABASE AND RETURNS EXPECTED FILES VIA API**********");
        if (!"VH".equalsIgnoreCase(folder)) {
            getFilenamesCompletedOrInProgress(folder)
                .assertThat().log().all()
                .statusCode(200)
                .body("folder-name", equalTo(folder))
                .body("filenames", hasItems(filenames.toArray()));
        }

        LOGGER.info("*****************************");
        LOGGER.info("*****************************");

        CaseDetails caseDetails = findCaseWithAutoRetryWithUserWithSearcherRole(caseRef);


        Map<String, Object> data = caseDetails.getData();
        LOGGER.info("data size: " + data.size());
        List recordingFiles = (ArrayList) data.get("recordingFiles");
        assertThat(recordingFiles.size()).isEqualTo(segmentCount);
        String hearingSource = (String)data.get("hearingSource");
        assertThat(hearingSource).isEqualTo("VH".equalsIgnoreCase(folder) ? "VH" : "CVP");
        LOGGER.info("num recordings: " + recordingFiles.size());

        Map ttlObject = (Map)data.get("TTL");
        LocalDate creationDate = LocalDate.parse(DATE);
        if (ttlEnabled) {
            assertThat(ttlObject.get("SystemTTL")).isEqualTo(ttlObject.get("OverrideTTL"));
            assertThat(ttlObject.get("Suspended")).isEqualTo("No");
            String ttl = (String) ttlObject.get("SystemTTL");
            assertThat(LocalDate.parse(ttl)).isGreaterThan(creationDate.plusYears(defaultTTL.getYears()).minusDays(2));
            assertThat(LocalDate.parse(ttl)).isLessThan(creationDate.plusYears(defaultTTL.getYears()).plusDays(2));
        } else {
            assertThat(ttlObject == null);
        }
    }

}
