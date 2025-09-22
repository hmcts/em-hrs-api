package uk.gov.hmcts.reform.em.hrs.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"folder_id", "recordingRef"})
})
public class HearingRecording {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String createdBy;

    @CreatedBy
    private String createdByService;

    private String lastModifiedBy;

    @LastModifiedBy
    private String lastModifiedByService;

    @LastModifiedDate
    private LocalDateTime modifiedOn;

    @CreatedDate
    private LocalDateTime createdOn;

    private boolean deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    private Folder folder;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecording")
    private Set<HearingRecordingAuditEntry> auditEntries;


    private LocalDate ttl;
    private String recordingRef;
    private String caseRef;
    private String hearingLocationCode;
    private String hearingRoomRef;
    private String hearingSource;
    private String jurisdictionCode;
    private String serviceCode;
    @Column(unique = true)
    private Long ccdCaseId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecording")
    private Set<HearingRecordingSegment> segments;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecording")
    private Set<HearingRecordingSharee> sharees;

    public HearingRecording() {
        // jpa/Hibernate need this
    }

    public static class HearingRecordingBuilder {
        public HearingRecordingBuilder modifiedOn(LocalDateTime modifiedOn) {
            this.modifiedOn = modifiedOn;
            return this;
        }

        public HearingRecordingBuilder createdOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }

}
