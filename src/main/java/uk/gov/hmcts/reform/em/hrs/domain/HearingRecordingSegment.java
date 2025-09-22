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

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class HearingRecordingSegment {

    @ManyToOne(fetch = FetchType.LAZY)
    private HearingRecording hearingRecording;

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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecordingSegment")
    private Set<HearingRecordingSegmentAuditEntry> auditEntries;

    private String blobUuid; //32 char?

    @Column(unique = true)
    private String filename;
    private String fileExtension;
    private String fileMd5Checksum; // char(32),
    private Long fileSizeMb; // numeric(2),

    private String ingestionFileSourceUri;

    private Integer recordingLengthMins;
    private Integer recordingSegment;
    private String interpreter;


    public HearingRecordingSegment() {
        // for Jpa/hibernate
    }

    public static class HearingRecordingSegmentBuilder {
        public HearingRecordingSegmentBuilder modifiedOn(LocalDateTime modifiedOn) {
            this.modifiedOn = modifiedOn;
            return this;
        }

        public HearingRecordingSegmentBuilder createdOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }

}
