package uk.gov.hmcts.reform.em.hrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseRecordingFile {

    @JsonProperty("documentLink")
    private CaseDocument caseDocument;

    @JsonProperty("segmentNo")
    private String segmentNumber;

    @JsonProperty("fileSize")
    private float fileSize;
}
