package uk.gov.hmcts.reform.em.hrs.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecordingSharee")
    private Set<HearingRecordingShareeAuditEntry> auditEntries;

    public HearingRecordingSharee() {
    }

    public HearingRecordingSharee(final UUID id,
                                  final HearingRecording hearingRecording,
                                  final @NotNull String shareeEmail,
                                  final String sharedByRef,
                                  final LocalDateTime sharedOn,
                                  final Set<HearingRecordingShareeAuditEntry> auditEntries) {
        this.hearingRecording = hearingRecording;
        this.id = id;
        this.shareeEmail = shareeEmail;
        this.sharedByRef = sharedByRef;
        this.sharedOn = sharedOn;
        setAuditEntries(auditEntries);
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
