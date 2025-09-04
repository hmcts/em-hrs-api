package uk.gov.hmcts.reform.em.hrs.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
public class JobInProgress {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Folder folder;

    private String filename;

    @CreatedDate
    private LocalDateTime createdOn;

    public JobInProgress() {
    }

    public JobInProgress(UUID id, Folder folder, String filename, LocalDateTime createdOn) {
        this.id = id;
        this.folder = folder;
        this.filename = filename;
        this.createdOn = createdOn;
    }
}
