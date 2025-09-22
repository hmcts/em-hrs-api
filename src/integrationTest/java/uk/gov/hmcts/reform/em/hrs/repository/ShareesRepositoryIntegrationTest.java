package uk.gov.hmcts.reform.em.hrs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.hearingRecordingWithNoDataBuilder;

class ShareesRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {


    private ShareesRepository underTest;

    @Autowired
    public ShareesRepositoryIntegrationTest(ShareesRepository underTest) {
        this.underTest = underTest;
    }

    @Test
    void testShouldSaveSharee() {
        final HearingRecordingSharee hearingRecordingSharee = HearingRecordingSharee.builder()
            .hearingRecording(hearingRecordingWithNoDataBuilder())
            .shareeEmail(SHAREE_EMAIL_ADDRESS).build();

        final HearingRecordingSharee savedSharee = underTest.save(hearingRecordingSharee);

        assertThat(savedSharee).satisfies(x -> {
            assertThat(x.getShareeEmail()).isEqualTo(SHAREE_EMAIL_ADDRESS);
            assertThat(x.getHearingRecording()).isNotNull();
        });
    }

    @Test
    void testAuditingFieldsArePopulated() {
        final LocalDateTime preTest = LocalDateTime.now(Clock.systemDefaultZone());
        final HearingRecordingSharee hearingRecordingSharee = HearingRecordingSharee.builder()
            .hearingRecording(hearingRecordingWithNoDataBuilder())
            .shareeEmail(SHAREE_EMAIL_ADDRESS).build();

        final HearingRecordingSharee savedSharee = underTest.save(hearingRecordingSharee);

        final LocalDateTime postTest = LocalDateTime.now(Clock.systemDefaultZone());
        assertThat(savedSharee)
            .satisfies(x -> assertThat(x.getSharedOn()).isBetween(preTest, postTest));
    }
}
