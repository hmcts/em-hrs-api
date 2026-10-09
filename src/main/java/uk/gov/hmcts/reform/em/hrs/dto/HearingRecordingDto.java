package uk.gov.hmcts.reform.em.hrs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingRecordingDto {

    // Explicit kebab-case names: ignoreUnknown would otherwise silently drop fields if the
    // Boot kebab-case naming strategy were lost under Jackson 3.
    private String folder;

    @JsonProperty("case-ref")
    private String caseRef;

    @JsonProperty("recording-source")
    private HearingSource recordingSource;

    @JsonProperty("hearing-room-ref")
    private String hearingRoomRef;

    @JsonProperty("service-code")
    private String serviceCode;

    @JsonProperty("jurisdiction-code")
    private String jurisdictionCode;

    @JsonProperty("court-location-code")
    private String courtLocationCode;

    @JsonProperty("recording-ref")
    private String recordingRef;

    @JsonProperty("source-blob-url")
    private String sourceBlobUrl;

    @JsonProperty("url-domain")
    private String urlDomain;

    private String filename;

    @JsonProperty("filename-extension")
    private String filenameExtension;

    @JsonProperty("file-size")
    private Long fileSize;

    private int segment;

    @JsonProperty("check-sum")
    private String checkSum;

    private String interpreter;

    @JsonProperty("recording-date-time")
    @JsonFormat(pattern = "yyyy-MM-dd-HH.mm.ss.SSS")
    @DateTimeFormat(pattern = "yyyy-MM-dd-HH.mm.ss.SSS")
    private LocalDateTime recordingDateTime;
}
