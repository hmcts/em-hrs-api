package uk.gov.hmcts.reform.em.hrs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.hrs.model.TtlCcdObject;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorageImpl;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden Jackson 3 round trips for Boot 4 data-integrity gates.
 * MVC paths use kebab-case; CCD models use explicit {@code @JsonProperty} names.
 */
class JacksonDataIntegrityTest {

    private ObjectMapper mvcMapper;
    private ObjectMapper ccdMapper;

    @BeforeEach
    void setUp() {
        mvcMapper = JsonMapper.builder()
            .findAndAddModules()
            .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        ccdMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }

    @Test
    void hearingRecordingDto_roundTripsKebabCasePayload() throws Exception {
        String json = """
            {
              "folder": "audioStream1",
              "case-ref": "CR-001",
              "recording-source": "CVP",
              "hearing-room-ref": "Room-1",
              "service-code": "PROBATE",
              "jurisdiction-code": "HRS",
              "court-location-code": "123",
              "recording-ref": "audioStream1/HRS-123-CR-001_2020-01-01-10.00.00.000",
              "source-blob-url": "https://example/blob",
              "url-domain": "https://hrs.example",
              "filename": "segment-0.mp4",
              "filename-extension": "mp4",
              "file-size": 2048,
              "segment": 0,
              "check-sum": "abc123",
              "interpreter": "none",
              "recording-date-time": "2020-01-01-10.00.00.000",
              "unknown-future-field": "ignored"
            }
            """;

        HearingRecordingDto dto = mvcMapper.readValue(json, HearingRecordingDto.class);

        assertThat(dto.getFolder()).isEqualTo("audioStream1");
        assertThat(dto.getCaseRef()).isEqualTo("CR-001");
        assertThat(dto.getRecordingSource()).isEqualTo(HearingSource.CVP);
        assertThat(dto.getHearingRoomRef()).isEqualTo("Room-1");
        assertThat(dto.getServiceCode()).isEqualTo("PROBATE");
        assertThat(dto.getJurisdictionCode()).isEqualTo("HRS");
        assertThat(dto.getCourtLocationCode()).isEqualTo("123");
        assertThat(dto.getRecordingRef()).isEqualTo("audioStream1/HRS-123-CR-001_2020-01-01-10.00.00.000");
        assertThat(dto.getSourceBlobUrl()).isEqualTo("https://example/blob");
        assertThat(dto.getUrlDomain()).isEqualTo("https://hrs.example");
        assertThat(dto.getFilename()).isEqualTo("segment-0.mp4");
        assertThat(dto.getFilenameExtension()).isEqualTo("mp4");
        assertThat(dto.getFileSize()).isEqualTo(2048L);
        assertThat(dto.getSegment()).isEqualTo(0);
        assertThat(dto.getCheckSum()).isEqualTo("abc123");
        assertThat(dto.getInterpreter()).isEqualTo("none");
        assertThat(dto.getRecordingDateTime()).isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 0, 0));

        String written = mvcMapper.writeValueAsString(dto);
        assertThat(written).contains("\"case-ref\"");
        assertThat(written).contains("\"recording-source\"");
        assertThat(written).contains("\"recording-date-time\"");
        assertThat(written).doesNotContain("\"caseRef\"");
        assertThat(written).doesNotContain("\"recordingSource\"");
    }

    @Test
    void hearingRecordingDto_doesNotBindCamelCaseAliases() throws Exception {
        String camelCaseJson = """
            {
              "caseRef": "CR-SHOULD-NOT-BIND",
              "recordingSource": "CVP",
              "folder": "audioStream1"
            }
            """;

        HearingRecordingDto dto = mvcMapper.readValue(camelCaseJson, HearingRecordingDto.class);

        assertThat(dto.getFolder()).isEqualTo("audioStream1");
        assertThat(dto.getCaseRef()).isNull();
        assertThat(dto.getRecordingSource()).isNull();
    }

    @Test
    void recordingFilenameDto_serializesFolderNameAsKebabCase() throws Exception {
        RecordingFilenameDto dto = new RecordingFilenameDto("folder-a", Set.of("a.mp4", "b.mp4"));

        String json = mvcMapper.writeValueAsString(dto);

        assertThat(json).contains("\"folder-name\":\"folder-a\"");
        assertThat(json).contains("\"filenames\"");
        assertThat(json).doesNotContain("\"folderName\"");
    }

    @Test
    void caseHearingRecording_roundTripsCcdPropertyNamesWithoutKebabStrategy() throws Exception {
        CaseHearingRecording recording = CaseHearingRecording.builder()
            .hearingSource("CVP")
            .hearingRoomRef("001")
            .recordingDate(LocalDate.of(2020, 1, 2))
            .recordingTimeOfDay("AM")
            .serviceCode("PROBATE")
            .jurisdictionCode("HRS")
            .courtLocationCode("CRY")
            .recordingReference("ref-1")
            .shareeEmail("a@b.com")
            .timeToLive(TtlCcdObject.builder()
                .suspended("No")
                .systemTTL("2030-01-01")
                .overrideTTL(null)
                .build())
            .build();

        JsonNode node = ccdMapper.convertValue(recording, JsonNode.class);

        assertThat(node.get("hearingSource").asText()).isEqualTo("CVP");
        assertThat(node.get("hearingRoomRef").asText()).isEqualTo("001");
        assertThat(node.get("recordingDate").asText()).isEqualTo("2020-01-02");
        assertThat(node.get("recipientEmailAddress").asText()).isEqualTo("a@b.com");
        assertThat(node.get("TTL").get("Suspended").asText()).isEqualTo("No");
        assertThat(node.get("TTL").get("SystemTTL").asText()).isEqualTo("2030-01-01");
        assertThat(node.has("hearing-source")).isFalse();

        CaseHearingRecording roundTrip = ccdMapper.convertValue(node, CaseHearingRecording.class);
        assertThat(roundTrip.getHearingSource()).isEqualTo("CVP");
        assertThat(roundTrip.getShareeEmail()).isEqualTo("a@b.com");
        assertThat(roundTrip.getTimeToLive().getSystemTTL()).isEqualTo("2030-01-01");
    }

    @Test
    void caseDocumentAndRecordingFile_preserveSnakeAndCamelCcdNames() throws Exception {
        CaseRecordingFile segment = CaseRecordingFile.builder()
            .caseDocument(CaseDocument.builder()
                .url("https://hrs/doc")
                .binaryUrl("https://hrs/doc/binary")
                .filename("file.mp4")
                .build())
            .segmentNumber("0")
            .fileSize("1.5")
            .build();

        JsonNode node = ccdMapper.convertValue(segment, JsonNode.class);

        assertThat(node.at("/documentLink/document_url").asText()).isEqualTo("https://hrs/doc");
        assertThat(node.at("/documentLink/document_binary_url").asText()).isEqualTo("https://hrs/doc/binary");
        assertThat(node.at("/documentLink/document_filename").asText()).isEqualTo("file.mp4");
        assertThat(node.get("segmentNo").asText()).isEqualTo("0");
        assertThat(node.get("fileSize").asText()).isEqualTo("1.5");
        assertThat(node.has("document-link")).isFalse();
    }

    @Test
    void storageReportAndBlobDetail_serializeKebabCaseApiFields() throws Exception {
        StorageReport report = new StorageReport(
            LocalDate.of(2020, 1, 1),
            new StorageReport.HrsSourceVsDestinationCounts(10, 9, 2, 1)
        );
        String reportJson = mvcMapper.writeValueAsString(report);
        assertThat(reportJson).contains("\"cvp-item-count\":10");
        assertThat(reportJson).contains("\"hrs-cvp-item-count\":9");
        assertThat(reportJson).contains("\"cvp-item-count-today\":2");
        assertThat(reportJson).contains("\"hrs-cvp-item-count-today\":1");
        assertThat(reportJson).doesNotContain("\"cvpItemCount\"");

        HearingRecordingStorageImpl.BlobDetail detail = new HearingRecordingStorageImpl.BlobDetail(
            "https://blob",
            42L,
            OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );
        String detailJson = mvcMapper.writeValueAsString(detail);
        assertThat(detailJson).contains("\"blob-url\"");
        assertThat(detailJson).contains("\"blob-size\":42");
        assertThat(detailJson).contains("\"last-modified\"");
        assertThat(detailJson).doesNotContain("\"blobUrl\"");
    }

    @Test
    void ccdMapper_isIsolatedFromMvcKebabCaseStrategy() throws Exception {
        CaseHearingRecording recording = CaseHearingRecording.builder()
            .hearingSource("CVP")
            .recordingReference("ref")
            .build();

        String mvcWritten = mvcMapper.writeValueAsString(recording);
        String ccdWritten = ccdMapper.writeValueAsString(recording);

        assertThat(ccdWritten).contains("\"hearingSource\"");
        assertThat(ccdWritten).doesNotContain("\"hearing-source\"");
        // MVC mapper still honours @JsonProperty explicit names on CCD models
        assertThat(mvcWritten).contains("\"hearingSource\"");
        assertThat(mvcWritten).doesNotContain("\"hearing-source\"");
    }
}
