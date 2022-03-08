package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShareesRepository extends JpaRepository<HearingRecordingSharee, UUID> {

    List<HearingRecordingSharee> findByShareeEmailIgnoreCase(String shareeEmail);
}

