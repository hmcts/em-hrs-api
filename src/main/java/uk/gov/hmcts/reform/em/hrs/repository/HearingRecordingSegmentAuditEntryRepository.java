package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;

import java.util.UUID;

@Repository
public interface HearingRecordingSegmentAuditEntryRepository
    extends CrudRepository<HearingRecordingSegmentAuditEntry, UUID> {


    @Query("""
            DELETE FROM HearingRecordingSegmentAuditEntry ae
            WHERE ae.hearingRecordingSegment.id = :hearingRecordingSegmentId
            """)
    void deleteByHearingRecordingSegmentId(@Param("hearingRecordingSegmentId") UUID hearingRecordingSegmentId);
}
