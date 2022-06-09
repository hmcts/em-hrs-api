package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.models.BlobRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.helper.AzureIntegrationTestOperations;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {
    TestAzureStorageConfig.class,
    BlobstoreClientImpl.class,
    AzureIntegrationTestOperations.class
})
class BlobstoreClientImplTest {
    private static final String ONE_ITEM_FOLDER = "one-item-folder";
    private static final String TEST_DATA = "Hello World!";
    private static final String PARTIAL_TEST_DATA = TEST_DATA.substring(0, 3);
    @Autowired
    private AzureIntegrationTestOperations azureIntegrationTestOperations;
    @Autowired
    private BlobstoreClientImpl underTest;

    @BeforeEach
    void setup() {
        azureIntegrationTestOperations.clearContainer();
    }

    @Test
    void testShouldDownloadFile() throws Exception {
        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID() + ".txt";
        azureIntegrationTestOperations.populateHrsContainer(filePath, TEST_DATA);

        BlobRange blobRange = null;
        try (final PipedInputStream pipedInput = new PipedInputStream();
             final PipedOutputStream output = new PipedOutputStream(pipedInput)) {
            underTest.downloadFile(filePath, null, output);
            assertThat(pipedInput).satisfies(this::assertStreamContent);
        }

        try (final PipedInputStream pipedInput = new PipedInputStream();
             final PipedOutputStream output = new PipedOutputStream(pipedInput)) {
            blobRange = new BlobRange(0, 3L);
            underTest.downloadFile(filePath, blobRange, output);
            assertThat(pipedInput).satisfies(this::assertPartialStreamContent);
        }
    }

    @Test
    void testShouldFetchBlobInfo() throws Exception {
        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID() + ".txt";
        azureIntegrationTestOperations.populateHrsContainer(filePath, TEST_DATA);
        try (final PipedInputStream pipedInput = new PipedInputStream();
             final PipedOutputStream output = new PipedOutputStream(pipedInput)) {
            underTest.downloadFile(filePath, null, output);
            underTest.fetchBlobInfo(filePath);
            assertThat(pipedInput).satisfies(this::assertStreamContent);
        }
    }

    private void assertStreamContent(final InputStream input) {
        final StringBuilder sb = new StringBuilder();
        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> {
                while (true) {
                    sb.append((char) input.read());
                    final String s = sb.toString();
                    if (s.contains(TEST_DATA)) {
                        break;
                    }
                }
                return true;
            });
        } finally {
            assertThat(sb.toString()).isEqualTo(TEST_DATA);
        }
    }

    private void assertPartialStreamContent(final InputStream input) {
        final StringBuilder sb = new StringBuilder();
        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> {
                while (true) {
                    sb.append((char) input.read());
                    final String s = sb.toString();
                    if (s.contains(PARTIAL_TEST_DATA)) {
                        break;
                    }
                }
                return true;
            });
        } finally {
            assertThat(sb.toString()).isEqualTo(PARTIAL_TEST_DATA);
        }
    }

}
