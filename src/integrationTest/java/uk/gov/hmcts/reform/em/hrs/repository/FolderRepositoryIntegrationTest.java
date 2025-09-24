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
    private FolderRepository underTest;

    @Autowired
    public FolderRepositoryIntegrationTest(FolderRepository underTest) {
        this.underTest = underTest;
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
