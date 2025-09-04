package uk.gov.hmcts.reform.em.hrs.domain;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@DiscriminatorColumn(name = "type")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AuditEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuditActions action;

    private String username;

    private String ipAddress;

    @NotNull
    private String serviceName;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventDateTime;

    private Long caseId;

    public Date getEventDateTime() {
        if (eventDateTime == null) {
            return null;
        } else {
            return new Date(eventDateTime.getTime());
        }
    }

    public void setEventDateTime(Date eventDateTime) {
        if (eventDateTime == null) {
            throw new IllegalArgumentException();
        } else {
            this.eventDateTime = new Date(eventDateTime.getTime());
        }
    }
}
