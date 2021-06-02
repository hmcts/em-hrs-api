package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.model.CaseDocument;
import uk.gov.hmcts.reform.em.hrs.model.CaseHearingRecording;
import uk.gov.hmcts.reform.em.hrs.model.CaseRecordingFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CaseDataContentCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDataContentCreator.class);

    private final ObjectMapper objectMapper;

    public CaseDataContentCreator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode createCaseStartData(final HearingRecordingDto hearingRecordingDto, final UUID recordingId) {

        CaseHearingRecording caseRecording = CaseHearingRecording.builder()
            .recordingFiles(Collections.singletonList(
                Map.of("value", createSegment(hearingRecordingDto, recordingId))
            ))
            .recordingDate(
                Optional.ofNullable(hearingRecordingDto.getRecordingDateTime())
                    .map(LocalDateTime::toLocalDate).orElse(null)
            )
            .recordingTimeOfDay(getRecordingTimeOfDay(hearingRecordingDto))
            .hearingSource(hearingRecordingDto.getRecordingSource())
            .hearingRoomRef(hearingRecordingDto.getHearingRoomRef())
            .serviceCode(hearingRecordingDto.getServiceCode())
            .jurisdictionCode(hearingRecordingDto.getJurisdictionCode())
            .courtLocationCode(hearingRecordingDto.getCourtLocationCode())
            .recordingReference(hearingRecordingDto.getCaseRef())
            .build();
        return objectMapper.convertValue(caseRecording, JsonNode.class);
    }

    public JsonNode createCaseUpdateData(final Map<String, Object> caseData, final UUID recordingId,
                                                    final HearingRecordingDto hearingRecordingDto) {

        CaseHearingRecording caseRecording = getCaseRecordingObject(caseData);

        boolean segmentNotYetAdded = extractCaseDocuments(caseRecording).stream()
            .noneMatch(caseDocument -> caseDocument.getFilename().equals(hearingRecordingDto.getFilename()));

        if (segmentNotYetAdded) {
            caseRecording.addRecordingFile(createSegment(hearingRecordingDto, recordingId));
        }
        return objectMapper.convertValue(caseRecording, JsonNode.class);
    }

    public CaseHearingRecording getCaseRecordingObject(final Map<String, Object> caseData) {
        return objectMapper.convertValue(caseData, CaseHearingRecording.class);
    }

    public List<CaseDocument> extractCaseDocuments(final CaseHearingRecording caseData) {
        return caseData.getRecordingFiles().stream()
            .map(mapItem -> mapItem.get("value"))
            .map(value -> objectMapper.convertValue(value, CaseRecordingFile.class))
            .map(recordingFile -> recordingFile.getCaseDocument())
            .collect(Collectors.toList());
    }


    private CaseRecordingFile createSegment(HearingRecordingDto hearingRecordingDto, UUID recordingId) {

        String documentUrl = String.format("%s/hearing-recordings/%s/segments/%d",
                                           hearingRecordingDto.getUrlDomain(), recordingId,
                                           hearingRecordingDto.getSegment());

        LOGGER.info("creating recording segment with url({})", documentUrl);

        CaseDocument recordingFile = CaseDocument.builder()
            .filename(hearingRecordingDto.getFilename())
            .url(documentUrl)
            .binaryUrl(documentUrl)
            .build();

        return CaseRecordingFile.builder()
            .caseDocument(recordingFile)
            .segmentNumber(String.valueOf(hearingRecordingDto.getSegment()))
            .fileSize(hearingRecordingDto.getFileSize() / (1024 * 1024))
            .build();
    }

    private String getRecordingTimeOfDay(HearingRecordingDto hearingRecordingDto) {
        LOGGER.info("setting time of day from recording time ({}))", hearingRecordingDto.getRecordingDateTime());
        return Optional.ofNullable(hearingRecordingDto.getRecordingDateTime())
            .map(dateTime -> dateTime.getHour() < 12 ? "AM" : "PM").orElse("");
    }
}
