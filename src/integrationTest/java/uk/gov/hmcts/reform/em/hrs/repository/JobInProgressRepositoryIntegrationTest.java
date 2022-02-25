package uk.gov.hmcts.reform.em.hrs.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobInProgressRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private JobInProgressRepository underTest;

    @BeforeEach
    void prepare() {
        final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        final LocalDateTime yesterday = now.minusDays(1);

        final List<JobInProgress> jobsInProgress = List.of(
            JobInProgress.builder()
                .id(UUID.randomUUID())
                .filename("a-yesterday.txt")
                .createdOn(yesterday)
                .build(),
            JobInProgress.builder()
                .id(UUID.randomUUID())
                .filename("b-yesterday.txt")
                .createdOn(yesterday)
                .build(),
            JobInProgress.builder()
                .id(UUID.randomUUID())
                .filename("c-today.txt")
                .createdOn(now)
                .build(),
            JobInProgress.builder()
                .id(UUID.randomUUID())
                .filename("d-NULL.txt")
                .build()
        );

        underTest.saveAll(jobsInProgress);

        final Iterable<JobInProgress> all = underTest.findAll();
        assertThat(all).hasSize(4);
    }

    @Test
    void testShouldDeleteJobsOlderThanTwentyFourHours() {
        final LocalDateTime twentyFourHoursAgo = LocalDateTime.now(Clock.systemUTC()).minusHours(24);

        underTest.deleteByCreatedOnLessThan(twentyFourHoursAgo);

        final Iterable<JobInProgress> actualJobs = underTest.findAll();
        assertThat(actualJobs).singleElement().satisfies(x -> assertThat(x.getFilename()).isEqualTo("c-today.txt"));
    }

}
