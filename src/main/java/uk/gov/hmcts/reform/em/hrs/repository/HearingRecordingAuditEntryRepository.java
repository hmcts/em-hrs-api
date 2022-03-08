package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingAuditEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface HearingRecordingAuditEntryRepository
    extends JpaRepository<HearingRecordingAuditEntry, UUID> {

    List<HearingRecordingAuditEntry> findByHearingRecordingOrderByEventDateTimeAsc(HearingRecording hearingRecording);

}
