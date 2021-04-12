package uk.gov.hmcts.reform.em.hrs.componenttests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestUtil {
    private static final String DOWNLOAD_URL_PREFIX = "https://SOMEPREFIXTBD";

    public static final String FILE_1 = "file-1.mp4";
    public static final String FILE_2 = "file-2.mp4";
    public static final String FILE_3 = "file-3.mp4";
    public static final String TEST_FOLDER_NAME = "folder-1";
    public static final UUID RANDOM_UUID = UUID.randomUUID();
    public static final String AUTHORIZATION_TOKEN = "xxx";
    public static final String SERVICE_AUTHORIZATION_TOKEN = "xxx";
    public static final Long CCD_CASE_ID = 1234L;
    public static final String SHAREE_EMAIL_ADDRESS = "sharee.tester@test.com";
    public static final String SHARER_EMAIL_ADDRESS = "sharer.tester@test.com";
    public static final UUID SHAREE_ID = UUID.randomUUID();
    public static final String CASE_REFERENCE = "hrs-grant-" + SHAREE_ID;
    public static final LocalDateTime RECORDING_DATETIME = LocalDateTime.now();
    public static final String RECORDING_REFERENCE = "file-1";

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

    public static final HearingRecordingDto HEARING_RECORDING_DTO = new HearingRecordingDto(
        CASE_REFERENCE,
        "CVP",
        "123",
        null,
        "JC",
        "LC",
        RECORDING_REFERENCE,
        "recording-cvp-uri",
        "hearing-recording-file-name",
        "mp4",
        123456789L,
        0,
        "erI2foA30B==",
        RECORDING_DATETIME
    );

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
        .name("name")
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
        .folder(Folder.builder().id(RANDOM_UUID).build())
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
        final ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(object);
    }
}
