package uk.gov.hmcts.reform.em.hrs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@Sql({"/data/create-folder.sql"})
class FolderRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {
    private static final String EMPTY_FOLDER = "folder-0";
    private static final String TEST_FOLDER = "folder-1";
    @Inject
    private FolderRepository underTest;
    @Inject
    private HearingRecordingSegmentRepository hearingRecordingSegmentRepository;

    // TODO - Move this into a separate HearingRecordingSegmentRepositoryTest file
    @Test
    void testShouldFindSegmentByRecordingId() {
        UUID hearingRecordingId = UUID.fromString("05A13771-58DF-4ABD-B62D-4A3F8DDF4286");
        final List<HearingRecordingSegment> hearingRecordingSegmentList =
            hearingRecordingSegmentRepository.findByHearingRecordingId(hearingRecordingId);

        assertThat(hearingRecordingSegmentList.get(0).getId().toString())
            .isEqualTo("8a4fdad2-d53e-40a2-ae88-6df8fbd6cc1d");
    }

    @Test
    void testShouldReturnEmptySetWhenDatabaseIsEmpty() {
        final Optional<Folder> folder = underTest.findByName(EMPTY_FOLDER);

        assertThat(folder).isEmpty();
    }

    @Test
    void testFindByFolderName() {
        final Optional<Folder> folder = underTest.findByName(TEST_FOLDER);

        assertThat(folder).hasValueSatisfying(x -> {
            assertThat(x.getId()).isEqualTo(UUID.fromString("3E3F63FB-3C7A-447B-86DA-69ED164763B0"));
            assertThat(x.getName()).isEqualTo(TEST_FOLDER);
            assertThat(x.getJobsInProgress().size()).isEqualTo(2);
            assertThat(x.getHearingRecordings().size()).isEqualTo(2);
        });
    }

}
