package uk.gov.hmcts.reform.em.hrs.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestApplicationConfig;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.exception.BlobNotFoundException;
import uk.gov.hmcts.reform.em.hrs.helper.AzureIntegrationTestOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.em.hrs.dto.HearingSource.CVP;

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
    void testShouldReturnEmptySetWhenFolderDoesNotExist() {
        final Set<String> files = underTest.findByFolderName(EMPTY_FOLDER);

        assertThat(files).isEmpty();
    }

    @Test
    void testShouldHandleTrailingSlashesGracefullyWhenFindingFilesInFolder() {
        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID() + ".txt";
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
        final String file = folder + "/" + UUID.randomUUID() + ".txt";
        azureIntegrationTestOperations.uploadToCvpContainer(file);
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file);

        HearingRecordingDto hrDto =
            HearingRecordingDto.builder().sourceBlobUrl(sourceUrl).filename(file).recordingSource(CVP).build();
        underTest.copyRecording(hrDto);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureIntegrationTestOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file));
    }

    @Test
    void testShouldEnsureOnlyTheSpecifiedCvpBlobIsCopiedToHrsBlobstore() {
        final String folder = UUID.randomUUID().toString();
        final String file1 = folder + "/" + UUID.randomUUID() + ".txt";
        final String file2 = folder + "/" + UUID.randomUUID() + ".txt";
        azureIntegrationTestOperations.populateCvpContainer(Set.of(file1, file2));
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file1);

        HearingRecordingDto hrDto =
            HearingRecordingDto.builder().sourceBlobUrl(sourceUrl).filename(file1).recordingSource(CVP).build();
        underTest.copyRecording(hrDto);

        await().atMost(TEN_SECONDS)
            .untilAsserted(() -> assertThat(azureIntegrationTestOperations.getHrsBlobsFrom(folder))
                .singleElement()
                .isEqualTo(file1));
    }

    @Test
    void testStorageReport() {
        StorageReport storageReport = underTest.getStorageReport();
        System.out.println(storageReport);
        assertEquals(storageReport.cvpItemCount, 0);
        assertEquals(storageReport.hrsCvpItemCount, 1);
    }

    @Test
    void testShouldEmitCopyFailedMessage() {
        final String file = UUID.randomUUID() + "/" + UUID.randomUUID() + ".txt";
        final String sourceUrl = azureIntegrationTestOperations.getBlobUrl(file);
        HearingRecordingDto hrDto =
            HearingRecordingDto.builder().sourceBlobUrl(sourceUrl).filename(file).build();
        assertThatExceptionOfType(BlobCopyException.class).isThrownBy(() -> underTest.copyRecording(hrDto));
    }

    @Test
    void testFindBlobShouldThrowBlobNotFoundException() {
        final String file = UUID.randomUUID() + ".txt";
        BlobNotFoundException exception =
            assertThrows(BlobNotFoundException.class,() ->  underTest.findBlob(HearingSource.CVP, file));

        String expectedMessage = "Resource with blobName: " + file + " could not be found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void testFindBlobShouldThrowHearingSourceException() {
        final String file = UUID.randomUUID() + ".txt";
        BlobNotFoundException exception =
            assertThrows(BlobNotFoundException.class, () -> underTest.findBlob(null, file));

        String expectedMessage = "Resource with hearingSource: null could not be found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void testShouldFindBlob() {
        final String file = UUID.randomUUID() + ".txt";
        String testData = "Test data - cvp data";
        var time = OffsetDateTime.now().minus(2, ChronoUnit.MINUTES);
        final String sourceUrl = azureIntegrationTestOperations.populateHrsCvpContainer(file, testData);

        var foundBlob = underTest.findBlob(HearingSource.CVP, file);
        assertThat(foundBlob.blobSize()).isEqualTo(testData.length());
        assertThat(foundBlob.blobUrl()).isEqualTo(sourceUrl);
        assertThat(foundBlob.lastModified()).isAfter(time);
        assertThat(foundBlob.lastModified()).isBefore(OffsetDateTime.now());
    }

    private Set<String> generateFilePaths() {
        final Random random = new Random();
        final int number = random.nextInt(8) + 2;

        return IntStream.rangeClosed(1, number)
            .mapToObj(x -> MANY_ITEMS_FOLDER + "/f" + x + ".txt")
            .collect(Collectors.toUnmodifiableSet());
    }
}
