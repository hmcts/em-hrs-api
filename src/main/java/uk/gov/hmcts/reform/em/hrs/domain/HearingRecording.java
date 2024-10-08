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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
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
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"folder_id", "recordingRef"})
})
public class HearingRecording {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
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

    private boolean ttlSet;

    @ManyToOne(fetch = FetchType.LAZY)
    private Folder folder;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecording")
    private Set<HearingRecordingAuditEntry> auditEntries;


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

    public HearingRecording(UUID id, String createdBy, String createdByService, String lastModifiedBy,
                            String lastModifiedByService,
                            LocalDateTime modifiedOn, LocalDateTime createdOn,
                            boolean deleted, boolean ttlSet, Folder folder,
                            Set<HearingRecordingAuditEntry> auditEntries,
                            String recordingRef, String caseRef, String hearingLocationCode,
                            String hearingRoomRef, String hearingSource,
                            String jurisdictionCode, String serviceCode, Long ccdCaseId,
                            Set<HearingRecordingSegment> segments) {
        setId(id);
        setCreatedBy(createdBy);
        setCreatedByService(createdByService);
        this.lastModifiedBy = lastModifiedBy;
        this.setLastModifiedByService(lastModifiedByService);
        setModifiedOn(modifiedOn);
        setCreatedOn(createdOn);
        setDeleted(deleted);
        setTtlSet(ttlSet);
        setFolder(folder);

        setAuditEntries(auditEntries);

        setRecordingRef(recordingRef);
        setCaseRef(caseRef);
        setHearingLocationCode(hearingLocationCode);
        setHearingRoomRef(hearingRoomRef);
        setHearingSource(hearingSource);
        setJurisdictionCode(jurisdictionCode);

        setServiceCode(serviceCode);
        setCcdCaseId(ccdCaseId);

        setSegments(segments);
    }

    public HearingRecording() {

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
