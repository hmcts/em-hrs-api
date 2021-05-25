package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    HearingRecordingDto hearingRecordingDto;
    CaseDataContentCreator underTest;

    @BeforeEach
    void setup() {
        String dateString = "1962-07-05-10.30.00.000";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
        hearingRecordingDto = HearingRecordingDto.builder()
            .caseRef(RECORDING_REF)
            .filename("recording-file-1")
            .recordingDateTime(LocalDateTime.parse(dateString, formatter))
            .jurisdictionCode("FM")
            .urlDomain("http://xui.com")
            .fileSize(123456789L)
            .segment(0)
            .build();

        underTest = new CaseDataContentCreator(objectMapper);
    }

    @Test
    void createCaseStartData() {

        JsonNode actual = underTest.createCaseStartData(hearingRecordingDto, RECORDING_ID);

        assertEquals("FM", actual.get("jurisdictionCode").asText());
        assertEquals(RECORDING_REF, actual.get("recordingReference").asText());
        assertEquals("JULY", actual.at("/recordingDate/month").asText());
        assertEquals("1962", actual.at("/recordingDate/year").asText());
        assertEquals("AM", actual.get("recordingTimeOfDay").asText());
        assertEquals(String.format("http://xui.com/hearing-recordings/%s/segments/0", RECORDING_ID),
                                actual.at("/recordingFiles/0/value/documentLink/document_url").asText());
    }

    @Test
    void createCaseUpdateData() {
        Map<String, CaseRecordingFile> valueMap = new HashMap<>();
        valueMap.put("value", CaseRecordingFile.builder()
            .caseDocument(CaseDocument.builder()
                .url("http://xui.com/hearing-recordings/12345/segments/1").filename("recording-file-2").build()
            ).build()
        );
        List<Map> segmentList = new ArrayList<>();
        segmentList.add(valueMap);
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("recordingFiles", segmentList);

        Map<String, Object> actual = underTest.createCaseUpdateData(caseData, RECORDING_ID, hearingRecordingDto);

        JsonNode resultNode = objectMapper.convertValue(actual, JsonNode.class);

        assertEquals("http://xui.com/hearing-recordings/12345/segments/1",
                     resultNode.at("/recordingFiles/0/value/documentLink/document_url").asText());
    }
}
