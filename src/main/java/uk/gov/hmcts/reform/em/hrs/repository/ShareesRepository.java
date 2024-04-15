package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShareesRepository extends CrudRepository<HearingRecordingSharee, UUID> {

    List<HearingRecordingSharee> findByShareeEmailIgnoreCase(String shareeEmail);

    @Modifying
    @Transactional
    @Query("DELETE FROM HearingRecordingSharee hrs WHERE hrs.hearingRecording.id = :hearingRecordingId")
    void deleteByHearingRecordingId(UUID hearingRecordingId);
}

