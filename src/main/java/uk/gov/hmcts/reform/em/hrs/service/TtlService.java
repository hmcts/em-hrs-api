package uk.gov.hmcts.reform.em.hrs.service;

import java.time.LocalDate;

public interface TtlService {

    LocalDate createTtl(String service, String jurisdiction, LocalDate createdDate);

    boolean isTtlEnabled();

    boolean hasTtlConfig(String serviceCode, String jurisdictionCode);

}
