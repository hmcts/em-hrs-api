package uk.gov.hmcts.reform.em.hrs.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class HearingRecordingSharee {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private HearingRecording hearingRecording;

    @NotNull
    private String shareeEmail;

    @CreatedBy
    private String sharedByRef;

    @CreatedDate
    private LocalDateTime sharedOn;

    public HearingRecordingSharee() {
    }

    public HearingRecordingSharee(final UUID id,
                                  final HearingRecording hearingRecording,
                                  final @NotNull String shareeEmail,
                                  final String sharedByRef,
                                  final LocalDateTime sharedOn) {
        this.hearingRecording = hearingRecording;
        this.id = id;
        this.shareeEmail = shareeEmail;
        this.sharedByRef = sharedByRef;
        this.sharedOn = sharedOn;
    }

    public static class HearingRecordingShareeBuilder {
        public HearingRecordingShareeBuilder hearingRecording(final HearingRecording hearingRecording) {
            this.hearingRecording = hearingRecording;
            return this;
        }

        public HearingRecordingShareeBuilder shareeEmail(final String shareeEmail) {
            this.shareeEmail = shareeEmail;
            return this;
        }
    }
}
