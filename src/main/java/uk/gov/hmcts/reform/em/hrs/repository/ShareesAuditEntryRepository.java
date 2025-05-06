package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface ShareesAuditEntryRepository
    extends CrudRepository<HearingRecordingShareeAuditEntry, UUID> {


    @Query("""
            DELETE FROM HearingRecordingShareeAuditEntry ae
            WHERE ae.hearingRecordingSharee.id IN :hearingRecordingShareeIds
            """)
    void deleteByHearingRecordingShareeIds(@Param("hearingRecordingShareeIds")
                                           Collection<UUID> hearingRecordingShareeIds);
}
