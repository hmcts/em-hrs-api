package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CaseDataContentCreator {

    private final ObjectMapper objectMapper;

    public CaseDataContentCreator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode createCaseStartData(final HearingRecordingDto hearingRecordingDto, final UUID recordingId) {

        CaseHearingRecording recording = CaseHearingRecording.builder()
            .recordingFiles(Collections.singletonList(
                Map.of("value", createSegment(hearingRecordingDto, recordingId))
            ))
            .recordingDateTime(hearingRecordingDto.getRecordingDateTime())
            .recordingTimeOfDay(getTimeOfDay(hearingRecordingDto.getRecordingDateTime()))
            .hearingSource(hearingRecordingDto.getRecordingSource())
            .hearingRoomRef(hearingRecordingDto.getHearingRoomRef())
            .serviceCode(hearingRecordingDto.getServiceCode())
            .jurisdictionCode(hearingRecordingDto.getJurisdictionCode())
            .courtLocationCode(hearingRecordingDto.getCourtLocationCode())
            .recordingReference(hearingRecordingDto.getRecordingRef())
            .build();
        return objectMapper.convertValue(recording, JsonNode.class);
    }

    public Map<String, Object> createCaseUpdateData(final Map<String, Object> caseData, final UUID recordingId,
                                                    final HearingRecordingDto hearingRecordingDto) {

        List<CaseRecordingFile> recordingFiles = extractRecordingFiles(caseData);

        boolean segmentNotYetAdded = extractRecordingFiles(caseData).stream()
            .map(recordingFile -> recordingFile.getCaseDocument())
            .noneMatch(caseDocument -> caseDocument.getFilename().equals(hearingRecordingDto.getFilename()));

        if (segmentNotYetAdded) {
            recordingFiles.add(createSegment(hearingRecordingDto, recordingId));

            caseData.put("recordingFiles", recordingFiles.stream()
                .map(recordingFile -> Map.of("value", recordingFile))
                .collect(Collectors.toList())
            );
        }
        return caseData;
    }

    public List<CaseRecordingFile> extractRecordingFiles(final Map<String, Object> caseData) {

        @SuppressWarnings("unchecked")
        List<Map> segmentNodes = (ArrayList) caseData.getOrDefault("recordingFiles", new ArrayList());

        return segmentNodes.stream()
            .map(segmentNode -> objectMapper.convertValue(segmentNode.get("value"), CaseRecordingFile.class))
            .collect(Collectors.toList());
    }

    private CaseRecordingFile createSegment(HearingRecordingDto hearingRecordingDto, UUID recordingId) {

        String documentUrl = String.format("%s/hearing-recordings/%s/segments/%d",
                                           hearingRecordingDto.getUrlDomain(), recordingId,
                                           hearingRecordingDto.getSegment());

        CaseDocument recordingFile = CaseDocument.builder()
            .filename(hearingRecordingDto.getFilename())
            .url(documentUrl)
            .binaryUrl(documentUrl)
            .build();

        return CaseRecordingFile.builder()
            .caseDocument(recordingFile)
            .segmentNumber(hearingRecordingDto.getSegment())
            .fileSize(hearingRecordingDto.getFileSize())
            .build();
    }

    private String getTimeOfDay(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.getHour() < 12 ? "AM" : "PM" : null;
    }
}
