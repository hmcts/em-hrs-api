package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface HearingRecordingAuditEntryRepository
    extends CrudRepository<HearingRecordingAuditEntry, UUID> {

    List<HearingRecordingAuditEntry> findByHearingRecordingOrderByEventDateTimeAsc(HearingRecording hearingRecording);

    @Modifying
    @Query("""
            DELETE FROM HearingRecordingAuditEntry ae
            WHERE ae.hearingRecording.id IN :hearingRecordingIds
            """)
    void deleteByHearingRecordingIds(@Param("hearingRecordingIds") Collection<UUID> hearingRecordingIds);
}
