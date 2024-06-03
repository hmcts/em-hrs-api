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
public class TTLObject {

    @JsonProperty("suspended")
    private String suspended;

    @JsonProperty("systemTTL")
    private String systemTTL;

    @JsonProperty("systemTTL")
    private String overrideTTL;
}
