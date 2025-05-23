package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEntryRepository
    extends CrudRepository<AuditEntry, UUID> {

    List<AuditEntry> findByEventDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

}
