package uk.gov.hmcts.reform.em.hrs.service;

import java.time.LocalDate;

public interface TtlService {

    LocalDate createTtl(String service, String jurisdiction, LocalDate createdDate);

    String hasTtlConfig(String serviceCode, String jurisdictionCode);
  
}
