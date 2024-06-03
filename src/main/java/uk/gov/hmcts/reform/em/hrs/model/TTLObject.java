package uk.gov.hmcts.reform.em.hrs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTLObject {

    @JsonProperty
    private String suspended;

    @JsonProperty
    private String systemTTL;

    @JsonProperty
    private String overrideTTL;
}
