package uk.gov.hmcts.reform.em.hrs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HearingRecordingTtlMigrationDTO(
        UUID id,
        LocalDateTime createdOn,
        String serviceCode,
        String jurisdictionCode,
        Long ccdCaseId
) {}
