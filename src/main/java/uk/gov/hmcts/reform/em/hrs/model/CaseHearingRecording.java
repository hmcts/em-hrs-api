package uk.gov.hmcts.reform.em.hrs.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseHearingRecording {

    @JsonProperty("hearingSource")
    private String hearingSource;

    @JsonProperty("hearingRoomRef")
    private String hearingRoomRef;

    @JsonProperty("recordingDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordingDate;

    @JsonProperty("recordingTimeOfDay")
    private String recordingTimeOfDay;

    @JsonProperty("serviceCode")
    private String serviceCode;

    @JsonProperty("jurisdictionCode")
    private String jurisdictionCode;

    @JsonProperty("courtLocationCode")
    private String courtLocationCode;

    @JsonProperty("recordingReference")
    private String recordingReference;

    @JsonProperty("recordingFiles")
    private List<Map<String, CaseRecordingFile>> recordingFiles;
}
