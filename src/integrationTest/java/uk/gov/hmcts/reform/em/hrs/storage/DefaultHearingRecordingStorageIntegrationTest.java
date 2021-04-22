package uk.gov.hmcts.reform.em.hrs.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestApplicationConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.helper.AzureOperations;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
    TestAzureStorageConfig.class,
    TestApplicationConfig.class,
    DefaultHearingRecordingStorage.class,
    AzureOperations.class}
)
class DefaultHearingRecordingStorageIntegrationTest {
    @Inject
    private Snooper snooper;
    @Inject
    private AzureOperations azureOperations;
    @Inject
    private DefaultHearingRecordingStorage underTest;

    @Captor
    private ArgumentCaptor<String> snoopCaptor;

    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);
    private static final String EMPTY_FOLDER = "folder-0";
    private static final String ONE_ITEM_FOLDER = "folder-1";
    private static final String MANY_ITEMS_FOLDER = "folder-2";

    @BeforeEach
    void setup() {
        snoopCaptor.getAllValues().clear();
        azureOperations.clearContainer();
    }

    @Test
    void testShouldReturnEmptySetWhenFolderDoesNotExist() {
        final Set<String> files = underTest.findByFolder(EMPTY_FOLDER);

        assertThat(files).isEmpty();
    }

    @Test
    void testShouldReturnASetContainingOneWhenFolderContainsOneItem() {
        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID().toString() + ".txt";
        azureOperations.uploadToHrsContainer(filePath);

        final Set<String> files = underTest.findByFolder(ONE_ITEM_FOLDER);

        assertThat(files).singleElement().isEqualTo(filePath);
    }

    @Test
    void testShouldReturnSetContainingMultipleFilenamesWhenFolderContainsMultipleItems() {
        final Set<String> filePaths = generateFilePaths();
        azureOperations.populateHrsContainer(filePaths);

        final Set<String> files = underTest.findByFolder(MANY_ITEMS_FOLDER);

        assertThat(files).hasSameElementsAs(filePaths);
    }

    @Test
    void testShouldEnsureTheSpecifiedCvpBlobAppearsInHrsBlobstore() {
        final String folder = UUID.randomUUID().toString();
        final String file = folder + "/" + UUID.randomUUID().toString() + ".txt";
        azureOperations.uploadToCvpContainer(file);
        final String sourceUrl = azureOperations.getBlobUrl(file);

        underTest.copyRecording(sourceUrl, file);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file));
    }

    @Test
    void testShouldEnsureOnlyTheSpecifiedCvpBlobIsCopiedToHrsBlobstore() {
        final String folder = UUID.randomUUID().toString();
        final String file1 = folder + "/" + UUID.randomUUID().toString() + ".txt";
        final String file2 = folder + "/" + UUID.randomUUID().toString() + ".txt";
        azureOperations.populateCvpContainer(Set.of(file1, file2));
        final String sourceUrl = azureOperations.getBlobUrl(file1);

        underTest.copyRecording(sourceUrl, file1);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file1));
    }

    @Test
    void testShouldEmitCopySuccessfulMessage() {
        final String file = UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString() + ".txt";
        azureOperations.uploadToCvpContainer(file);
        final String sourceUrl = azureOperations.getBlobUrl(file);

        underTest.copyRecording(sourceUrl, file);

        assertMessagesContain(String.format("File %s copied successfully", file));
    }

    @Test
    void testShouldEmitCopyFailedMessage() {
        final String file = UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString() + ".txt";
        final String sourceUrl = azureOperations.getBlobUrl(file);

        underTest.copyRecording(sourceUrl, file);

        assertMessagesContain(String.format("File %s copied failed:: ", file));
    }

    private void assertMessagesContain(final String message) {
        await().atMost(TEN_SECONDS).untilAsserted(() -> {
            verify(snooper, atLeastOnce()).snoop(snoopCaptor.capture());

            assertThat(snoopCaptor.getAllValues())
                .isNotEmpty()
                .satisfies(x -> assertThat(x.stream().anyMatch(y -> y.startsWith(message))).isTrue());
        });
    }

    private Set<String> generateFilePaths() {
        final Random random = new Random();
        final int number = random.nextInt(8) + 2;

        return IntStream.rangeClosed(1, number)
            .mapToObj(x -> MANY_ITEMS_FOLDER + "/f" + x + ".txt")
            .collect(Collectors.toUnmodifiableSet());
    }
}
