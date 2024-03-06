package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;
import uk.gov.hmcts.reform.em.hrs.util.FileNameCoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CaseDataContentCreatorTest {

    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final String RECORDING_REF = "FT-0111-testfile200M";

    private static ObjectMapper objectMapper;
    HearingRecordingDto hearingRecordingDto;
    CaseDataContentCreator underTest;

    private String fileName = "audiostream123/recording-file-1";
    private String fileNameEncoded =  FileNameCoder.encodeFileName(fileName);

    @BeforeEach
    void setup() {
        objectMapper = JsonMapper.builder() // or different mapper for other format
            .addModule(new ParameterNamesModule())
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .build();
        String dateString = "1962-07-05-10.30.00.000";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
        hearingRecordingDto = HearingRecordingDto.builder()
            .caseRef(RECORDING_REF)
            .recordingSource(HearingSource.CVP)
            .filename(fileName)
            .recordingDateTime(LocalDateTime.parse(dateString, formatter))
            .jurisdictionCode("FM")
            .urlDomain("http://xui.com")
            .fileSize(123456789L)
            .segment(0)
            .build();

        underTest = new CaseDataContentCreator(objectMapper);
    }

    @Test
    void testConvertCaseDataMapToCaseHearingRecordingObject() {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("hearingSource", "CVP");
        caseData.put("hearingRoomRef", "001");
        caseData.put("recordingTimeOfDay", "AM");
        caseData.put("serviceCode", "CVP");
        caseData.put("jurisdictionCode", "TRB");
        caseData.put("courtLocationCode", "CRY");
        caseData.put("recordingReference", "testFile200Mb");

        CaseHearingRecording caseHearingRecording = underTest.getCaseRecordingObject(caseData);

        assertEquals("001", caseHearingRecording.getHearingRoomRef());
    }

    @Test
    void createCaseStartData() {

        JsonNode actual = underTest.createCaseStartData(hearingRecordingDto, RECORDING_ID);

        assertEquals("FM", actual.get("jurisdictionCode").asText());
        assertEquals(RECORDING_REF, actual.get("recordingReference").asText());
        assertEquals("1962-07-05", actual.get("recordingDate").asText());
        assertEquals("AM", actual.get("recordingTimeOfDay").asText());
        assertEquals(
            String.format(
                "http://xui.com/hearing-recordings/%s/file/%s",
                RECORDING_ID,
                fileNameEncoded
            ),
            actual.at("/recordingFiles/0/value/documentLink/document_url").asText()
        );
    }

    @Test
    void createCaseUpdateData() {
        Map<String, CaseRecordingFile> valueMap = new HashMap<>();
        valueMap.put("value", CaseRecordingFile.builder()
            .caseDocument(CaseDocument.builder()
                .url("http://xui.com/hearing-recordings/12345/segments/0").filename("recording-file-2").build()
            ).build()
        );
        List<Map> segmentList = new ArrayList<>();
        segmentList.add(valueMap);
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("recordingFiles", segmentList);

        JsonNode actual = underTest.createCaseUpdateData(caseData, RECORDING_ID, hearingRecordingDto);

        JsonNode resultNode = objectMapper.convertValue(actual, JsonNode.class);

        assertEquals(
            "http://xui.com/hearing-recordings/12345/segments/0",
            resultNode.at("/recordingFiles/0/value/documentLink/document_url").asText()
        );

        assertEquals(
            "http://xui.com/hearing-recordings/" + RECORDING_ID + "/file/" + this.fileNameEncoded,
            resultNode.at("/recordingFiles/1/value/documentLink/document_url").asText()
        );
    }
}
