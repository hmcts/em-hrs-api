package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingShareeAuditEntry;

import java.util.UUID;

@Repository
public interface ShareesAuditEntryRepository
    extends PagingAndSortingRepository<HearingRecordingShareeAuditEntry, UUID> {
}
