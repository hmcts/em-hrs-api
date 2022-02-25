package uk.gov.hmcts.reform.em.hrs.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Builder
@Getter
@Setter
public class JobInProgress {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
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
