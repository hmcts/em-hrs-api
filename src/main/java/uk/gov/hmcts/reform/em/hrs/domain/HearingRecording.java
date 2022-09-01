package uk.gov.hmcts.reform.em.hrs.domain;

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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


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


    @ManyToOne(fetch = FetchType.LAZY)
    private Folder folder;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hearingRecording")
    private Set<HearingRecordingAuditEntry> auditEntries;


    private LocalDateTime ttl;
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
                            boolean deleted, Folder folder,
                            Set<HearingRecordingAuditEntry> auditEntries,
                            LocalDateTime ttl,
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
        setFolder(folder);

        setAuditEntries(auditEntries);
        setTtl(ttl);

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
