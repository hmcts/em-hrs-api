package uk.gov.hmcts.reform.em.hrs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTLObject {

    private String suspended;

    private String systemTTL;

    private String overrideTTL;
}
