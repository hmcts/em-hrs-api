package uk.gov.hmcts.reform.em.hrs.componenttests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestUtil {
    private static final String DOWNLOAD_URL_PREFIX = "https://xui/hearing-recordings/";

    public static final int INGESTION_QUEUE_SIZE = 2;
    public static final String FILE_1 = "file-1.mp4";
    public static final String FILE_2 = "file-2.mp4";
    public static final String FILE_3 = "file-3.mp4";
    public static final String TEST_FOLDER_NAME = "folder-1";
    public static final UUID RANDOM_UUID = UUID.randomUUID();
    public static final String AUTHORIZATION_TOKEN = "xxxxxxxxxxxxxxxxxxxx";
    public static final String SERVICE_AUTHORIZATION_TOKEN = "xxxxxxxxxxxxxxxx";
    public static final Long CCD_CASE_ID = 1234L;
    public static final String SHAREE_EMAIL_ADDRESS = "sharee.tester@test.com";
    public static final String SHARER_EMAIL_ADDRESS = "sharer.tester@test.com";
    public static final UUID SHAREE_ID = UUID.randomUUID();
    public static final String CASE_REFERENCE = "hrs-grant-" + SHAREE_ID;
    public static final LocalDateTime RECORDING_DATETIME = LocalDateTime.now();
    public static final LocalDate RECORDING_DATE = LocalDate.now();
    public static final String RECORDING_TIMEOFDAY = RECORDING_DATETIME.getHour() > 12 ? "AM" : "PM";
    public static final String RECORDING_REFERENCE = "file-1";
    public static final Folder TEST_FOLDER = Folder.builder().name(TEST_FOLDER_NAME).build();
    public static final String SERVER_ERROR_MESSAGE = "We have detected a problem and our engineers are working on it."
        + "\nPlease try again later and thank you for your patience";

    public static final HearingRecordingSegment SEGMENT_1 = HearingRecordingSegment.builder()
        .id(RANDOM_UUID)
        .filename(FILE_1)
        .build();
    private static final HearingRecordingSegment SEGMENT_2 = HearingRecordingSegment.builder()
        .id(RANDOM_UUID)
        .filename(FILE_2)
        .build();
    private static final HearingRecordingSegment SEGMENT_3 = HearingRecordingSegment.builder()
        .id(RANDOM_UUID)
        .filename(FILE_3)
        .build();

    public static final HearingRecordingDto HEARING_RECORDING_DTO = HearingRecordingDto.builder()
        .folder(TEST_FOLDER_NAME)
        .caseRef(CASE_REFERENCE)
        .recordingSource("CVP")
        .courtLocationCode("LC")
        .jurisdictionCode("JC")
        .hearingRoomRef("123")
        .recordingRef(RECORDING_REFERENCE)
        .filename("hearing-recording-file-name")
        .recordingDateTime(RECORDING_DATETIME)
        .filenameExtension("mp4")
        .fileSize(123456789L)
        .segment(0)
        .cvpFileUrl("recording-cvp-uri")
        .checkSum("erI2foA30B==")
        .build();

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        StandardCharsets.UTF_8
    );

    public static final Set<String> SEGMENTS_DOWNLOAD_LINKS = Set.of(
        String.format("%s/%s", DOWNLOAD_URL_PREFIX, SEGMENT_1.getFilename()),
        String.format("%s/%s", DOWNLOAD_URL_PREFIX, SEGMENT_2.getFilename()),
        String.format("%s/%s", DOWNLOAD_URL_PREFIX, SEGMENT_3.getFilename())
    );
    public static final List<String> RECORDING_SEGMENT_DOWNLOAD_URLS = List.copyOf(SEGMENTS_DOWNLOAD_LINKS);

    public static final Folder EMPTY_FOLDER = Folder.builder()
        .id(RANDOM_UUID)
        .name("EMPTY_FOLDER")
        .hearingRecordings(Collections.emptyList())
        .jobsInProgress(Collections.emptyList())
        .build();

    public static final Folder FOLDER = Folder.builder()
        .id(RANDOM_UUID)
        .hearingRecordings(List.of(HearingRecording.builder()
                                       .id(RANDOM_UUID)
                                       .segments(Collections.emptySet())
                                       .build()))
        .jobsInProgress(Collections.emptyList())
        .build();

    public static final Folder FOLDER_WITH_SEGMENT = Folder.builder()
        .id(RANDOM_UUID)
        .hearingRecordings(List.of(HearingRecording.builder()
                                       .id(RANDOM_UUID)
                                       .segments(Set.of(SEGMENT_1, SEGMENT_2, SEGMENT_3))
                                       .build()))
        .jobsInProgress(Collections.emptyList())
        .build();

    public static final Folder FOLDER_WITH_JOBS_IN_PROGRESS = Folder.builder()
        .id(RANDOM_UUID)
        .name(TEST_FOLDER_NAME)
        .hearingRecordings(Collections.emptyList())
        .jobsInProgress(List.of(
            JobInProgress.builder().filename(FILE_1).build(),
            JobInProgress.builder().filename(FILE_2).build()
        ))
        .build();

    public static final Folder FOLDER_WITH_SEGMENT_AND_IN_PROGRESS = Folder.builder()
        .id(RANDOM_UUID)
        .hearingRecordings(List.of(HearingRecording.builder()
                                       .id(RANDOM_UUID)
                                       .segments(Set.of(SEGMENT_1, SEGMENT_2))
                                       .build()))
        .jobsInProgress(List.of(JobInProgress.builder().filename(FILE_3).build()))
        .build();

    public static final HearingRecording HEARING_RECORDING = HearingRecording.builder()
        .id(RANDOM_UUID)
        .folder(TEST_FOLDER)
        .segments(Collections.emptySet())
        .build();

    public static final HearingRecording HEARING_RECORDING_WITH_SEGMENTS = HearingRecording.builder()
        .id(RANDOM_UUID)
        .caseRef(CASE_REFERENCE)
        .ccdCaseId(CCD_CASE_ID)
        .segments(Set.of(SEGMENT_1, SEGMENT_2, SEGMENT_3))
        .folder(Folder.builder().id(RANDOM_UUID).build())
        .createdOn(RECORDING_DATETIME)
        .build();

    public static final HearingRecordingSharee HEARING_RECORDING_SHAREE = HearingRecordingSharee.builder()
        .id(SHAREE_ID)
        .build();

    public static final CaseRecordingFile CASE_RECORDING_FILE = CaseRecordingFile.builder()
        .caseDocument(
            CaseDocument.builder()
                .url("https://xui.domain/hearing-recordings/1234/segments/0")
                .binaryUrl("https://xui.domain/hearing-recordings/1234/segments/0")
                .filename("filename")
                .build()
        )
        .fileSize(123456789L)
        .segmentNumber("0")
        .build();

    private TestUtil() {
    }

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        final ObjectMapper om = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om.writeValueAsBytes(object);
    }

    public static String convertObjectToJsonString(Object object) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        return objectMapper.writeValueAsString(object);
    }
}
