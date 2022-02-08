package uk.gov.hmcts.reform.em.hrs.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestApplicationConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.helper.AzureIntegrationTestOperations;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {
    TestAzureStorageConfig.class,
    TestApplicationConfig.class,
    HearingRecordingStorageImpl.class,
    AzureIntegrationTestOperations.class}
)
class DefaultHearingRecordingStorageIntegrationTest {
    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);
    private static final String EMPTY_FOLDER = "folder-0";
    private static final String ONE_ITEM_FOLDER = "folder-1";
    private static final String MANY_ITEMS_FOLDER = "folder-2";

    @Autowired
    private AzureIntegrationTestOperations azureIntegrationTestOperations;
    @Autowired
    private HearingRecordingStorageImpl underTest;
    @Captor
    private ArgumentCaptor<String> snoopCaptor;

    @BeforeEach
    void setup() {
        snoopCaptor.getAllValues().clear();
        azureIntegrationTestOperations.clearContainer();
    }

    @Test
    void testStorageReport() {
        final StorageReport report = underTest.getStorageReport();
        assertThat(report).isNotNull();
        assertThat(report.cvpItemCount).isEqualTo(1);
        assertThat(report.hrsItemCount).isEqualTo(1);
    }

    @Test
    void testShouldReturnEmptySetWhenFolderDoesNotExist() {
        final Set<String> files = underTest.findByFolderName(EMPTY_FOLDER);

        assertThat(files).isEmpty();
    }

    @Test
    void testShouldHandleTrailingSlashesGracefullyWhenFindingFilesInFolder() {
        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID().toString() + ".txt";
        azureIntegrationTestOperations.uploadToHrsContainer(filePath);

        final Set<String> filesWithoutTrailingSlash = underTest.findByFolderName(ONE_ITEM_FOLDER);
        final Set<String> filesWithTrailingSlash = underTest.findByFolderName(ONE_ITEM_FOLDER + "/");

        assertThat(filesWithoutTrailingSlash).singleElement().isEqualTo(filePath);
        assertThat(filesWithTrailingSlash).singleElement().isEqualTo(filePath);
    }

    @Test
    void testShouldFindCompleteSetOfUploadedFiles() {
        final Set<String> filePaths = generateFilePaths();
        azureIntegrationTestOperations.populateHrsContainer(filePaths);

        final Set<String> files = underTest.findByFolderName(MANY_ITEMS_FOLDER);

        assertThat(files).hasSameElementsAs(filePaths);
    }

    @Test
    void testShouldEnsureTheSpecifiedCvpBlobAppearsInHrsBlobstore() {
        final String folder = UUID.randomUUID().toString();
        final String file = folder + "/" + UUID.randomUUID().toString() + ".txt";
        azureIntegrationTestOperations.uploadToCvpContainer(file);
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file);

        underTest.copyRecording(sourceUrl, file);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureIntegrationTestOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file));
    }

    @Test
    void testShouldEnsureOnlyTheSpecifiedCvpBlobIsCopiedToHrsBlobstore() {
        final String folder = UUID.randomUUID().toString();
        final String file1 = folder + "/" + UUID.randomUUID().toString() + ".txt";
        final String file2 = folder + "/" + UUID.randomUUID().toString() + ".txt";
        azureIntegrationTestOperations.populateCvpContainer(Set.of(file1, file2));
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file1);

        underTest.copyRecording(sourceUrl, file1);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureIntegrationTestOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file1));
    }

    @Test
    void testShouldEmitCopyFailedMessage() {
        final String file = UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString() + ".txt";
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file);
        assertThatExceptionOfType(BlobCopyException.class).isThrownBy(() -> underTest.copyRecording(sourceUrl, file));
    }


    private Set<String> generateFilePaths() {
        final Random random = new Random();
        final int number = random.nextInt(8) + 2;

        return IntStream.rangeClosed(1, number)
            .mapToObj(x -> MANY_ITEMS_FOLDER + "/f" + x + ".txt")
            .collect(Collectors.toUnmodifiableSet());
    }
}
