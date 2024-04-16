package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;

import java.util.UUID;

@Repository
public interface ShareesAuditEntryRepository
    extends CrudRepository<HearingRecordingShareeAuditEntry, UUID> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM audit_entry where caseId = :caseId",
        nativeQuery = true)
    void deleteByCaseRef(long caseId);
}
