package uk.gov.hmcts.reform.em.hrs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql({"/data/create-folder.sql"})
class HearingRecordingSegmentRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private HearingRecordingSegmentRepository hearingRecordingSegmentRepository;

    @Test
    void testShouldFindSegmentByRecordingId() {
        UUID hearingRecordingId = UUID.fromString("05A13771-58DF-4ABD-B62D-4A3F8DDF4286");
        final List<HearingRecordingSegment> hearingRecordingSegmentList =
            hearingRecordingSegmentRepository.findByHearingRecordingId(hearingRecordingId);

        assertThat(hearingRecordingSegmentList.get(0).getId().toString())
            .isEqualTo("8a4fdad2-d53e-40a2-ae88-6df8fbd6cc1d");
    }


    @Test
    void testShouldFindSegmentsByFolderName() {
        Set<HearingRecordingSegment> set =
            hearingRecordingSegmentRepository.findByHearingRecordingFolderName("folder-1");

        assertThat(set.size()).isEqualTo(7);
    }
}
