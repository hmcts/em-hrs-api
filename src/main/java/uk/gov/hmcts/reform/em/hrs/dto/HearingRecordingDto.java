package uk.gov.hmcts.reform.em.hrs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingRecordingDto {

    private String folder;
    private String caseRef;
    private HearingSource recordingSource;
    private String hearingRoomRef;
    private String serviceCode;
    private String jurisdictionCode;
    private String courtLocationCode;
    private String recordingRef;
    private String sourceBlobUrl;
    private String urlDomain;
    private String filename;
    private String filenameExtension;
    private Long fileSize;
    private int segment;
    private String checkSum;
    private String interpreter;

    @JsonFormat(pattern = "yyyy-MM-dd-HH.mm.ss.SSS")
    @DateTimeFormat(pattern = "yyyy-MM-dd-HH.mm.ss.SSS")
    private LocalDateTime recordingDateTime;
}
