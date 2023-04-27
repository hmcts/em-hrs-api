package uk.gov.hmcts.reform.em.hrs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql({"/data/create-folder.sql"})
class FolderRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    private static final String EMPTY_FOLDER = "folder-0";
    private static final String TEST_FOLDER = "folder-1";
    private static final String TEST_FOLDER3 = "folder-3";
    private static final String CVP_HEARING_SOURCE = "CVP";

    @Autowired
    private FolderRepository underTest;

    @Test
    void testShouldReturnEmptySetWhenDatabaseIsEmpty() {
        final Optional<Folder> folder = underTest.findByName(EMPTY_FOLDER);

        assertThat(folder).isEmpty();
    }

    @Test
    void testShouldReturnEmptySetForHearingSourceWhenDatabaseIsEmpty() {
        final Optional<Folder> folder = underTest.findByNameAndHearingSource(TEST_FOLDER, "not_valid");

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

    @Test
    void testFindByFolderNameAndHearingSource() {
        final Optional<Folder> folder = underTest.findByNameAndHearingSource(TEST_FOLDER3, CVP_HEARING_SOURCE);

        assertThat(folder).hasValueSatisfying(x -> {
            assertThat(x.getId()).isEqualTo(UUID.fromString("7acb7529-ee08-4ad4-b989-d0b3ac536578"));
            assertThat(x.getName()).isEqualTo(TEST_FOLDER3);
            assertThat(x.getJobsInProgress().size()).isEqualTo(1);
            assertThat(x.getHearingRecordings().size()).isEqualTo(0);
        });
    }


}
