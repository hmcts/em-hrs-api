package uk.gov.hmcts.reform.em.hrs.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;
import java.util.UUID;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;



@Entity
@Getter
@Setter
@DiscriminatorColumn(name = "type")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AuditEntry {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
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
