package uk.gov.hmcts.reform.em.hrs.dto;

import java.util.UUID;

public record HearingRecordingDeletionDto(
        UUID hearingRecordingId,
        UUID hearingRecordingSegmentId,
        UUID shareeId,
        String hearingSource,
        String filename

) {
}
