package uk.gov.hmcts.reform.em.hrs.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
@Builder
@Getter
@Setter
//@EntityListeners(AuditingEntityListener.class)
public class HearingRecordingSegment {

    @ManyToOne(fetch = FetchType.LAZY)
    private HearingRecording hearingRecording;

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


    public HearingRecordingSegment(HearingRecording hearingRecording, UUID id, String createdBy,
                                   String createdByService, String lastModifiedBy,
                                   String lastModifiedByService,
                                   LocalDateTime modifiedOn,
                                   LocalDateTime createdOn,
                                   boolean deleted,
                                   Set<HearingRecordingSegmentAuditEntry> auditEntries,
                                   String blobUuid, String filename,
                                   String fileExtension, String fileMd5Checksum, Long fileSizeMb,
                                   String ingestionFileSourceUri,
                                   Integer recordingLengthMins,
                                   Integer recordingSegment) {
        setHearingRecording(hearingRecording);
        setId(id);
        setCreatedBy(createdBy);
        setCreatedByService(createdByService);
        this.lastModifiedBy = lastModifiedBy;
        this.setLastModifiedByService(lastModifiedByService);
        setModifiedOn(modifiedOn);
        setCreatedOn(createdOn);
        setDeleted(deleted);


        setAuditEntries(auditEntries);
        //setRoles(roles);

        setBlobUuid(blobUuid);
        setFilename(filename);

        setFileExtension(fileExtension);
        setFileMd5Checksum(fileMd5Checksum);
        setFileSizeMb(fileSizeMb);

        setIngestionFileSourceUri(ingestionFileSourceUri);


        setRecordingLengthMins(recordingLengthMins);
        setRecordingSegment(recordingSegment);
    }

    public HearingRecordingSegment() {
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
