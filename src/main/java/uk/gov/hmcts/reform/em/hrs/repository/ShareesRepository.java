package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShareesRepository extends CrudRepository<HearingRecordingSharee, UUID> {

    List<HearingRecordingSharee> findByShareeEmailIgnoreCase(String shareeEmail);

    @Modifying
    @Query("""
            DELETE FROM HearingRecordingSharee hrs
            WHERE hrs.hearingRecording.id IN :hearingRecordingIds
            """)
    void deleteByHearingRecordingIds(@Param("hearingRecordingIds") Collection<UUID> hearingRecordingIds);

    @Query("SELECT hrs.id FROM HearingRecordingSharee hrs WHERE hrs.hearingRecording.id IN :hearingRecordingIds")
    List<UUID> findAllByHearingRecordingIds(List<UUID> hearingRecordingIds);
}

