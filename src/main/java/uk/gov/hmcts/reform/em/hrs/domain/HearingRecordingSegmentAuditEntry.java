package uk.gov.hmcts.reform.em.hrs.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@DiscriminatorValue(value = "hearing_recording_segment")
public class HearingRecordingSegmentAuditEntry extends AuditEntry {

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private HearingRecordingSegment hearingRecordingSegment;

    public HearingRecordingSegmentAuditEntry(HearingRecordingSegment hearingRecordingSegment) {
        super();
        this.hearingRecordingSegment = hearingRecordingSegment;
    }
}
