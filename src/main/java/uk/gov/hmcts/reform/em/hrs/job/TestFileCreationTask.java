package uk.gov.hmcts.reform.em.hrs.job;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.config.TTLMapperConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(name = "scheduling.task.test-file-creation.enabled")
public class TestFileCreationTask {

    @Value("${scheduling.task.test-file-creation.test-cases}")
    private int testCasesToCreate;

    @Value("${scheduling.task.test-file-creation.max-concurrent-uploads}")
    private int maxConcurrentUploads;

    private static final String[] LOCATION_CODES = {"0372", "0266"};
    private static final String TASK_NAME = "test-file-upload";
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileCreationTask.class);

    private final TTLMapperConfig ttlMapperConfig;
    private final BlobContainerClient cvpBlobContainerClient;

    @Autowired
    public TestFileCreationTask(TTLMapperConfig ttlMapperConfig,
                                @Qualifier("CvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient) {
        this.ttlMapperConfig = ttlMapperConfig;
        this.cvpBlobContainerClient = cvpBlobContainerClient;
    }

    @Scheduled(cron = "${scheduling.task.test-file-creation.schedule}")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        LOGGER.info("Started {} job", TASK_NAME);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        File inputFile = new File(Objects.requireNonNull(
            getClass().getClassLoader().getResource("test-upload-file.mp4")).getFile());
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("Invalid input file path");
        }

        List<String> fileNames = new ArrayList<>();

        for (int i = 0; i < testCasesToCreate; i++) {
            String caseRef = "Test" + i * 1000;
            boolean includeLocationCode = i % 3 == 1;
            String segment0 = "audiostream6247/" + generateFileName(caseRef, "0", includeLocationCode);
            fileNames.add(segment0);

            // Create an additional file with segment 1 for 10% of the files
            if (i % 10 == 0) {
                String segment1 = "audiostream6247/" + generateFileName(caseRef, "1", includeLocationCode);
                fileNames.add(segment1);
            }

            if (fileNames.size() >= maxConcurrentUploads) {
                uploadFilesToBlobStorage(inputFile, fileNames);
                fileNames.clear();
            }
        }

        if (!fileNames.isEmpty()) {
            uploadFilesToBlobStorage(inputFile, fileNames);
        }

        stopWatch.stop();
        LOGGER.info("Test file creation job took {} ms", stopWatch.getDuration().toMillis());
        LOGGER.info("Finished {} job", TASK_NAME);
    }

    private String generateFileName(String caseRef, String segment, boolean includeLocationCode) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String serviceCode = getRandomElement(ttlMapperConfig.getTtlServiceMap().keySet().toArray(new String[0]));
        String datePart = today.format(dateFormatter) + "-12.00.00.000";
        String locationCodePart = includeLocationCode ? "-" + getRandomElement(LOCATION_CODES) : "";
        return String.format("%s%s-%s_%s-%s_%s.mp4",
                             serviceCode,
                             locationCodePart,
                             caseRef,
                             datePart,
                             "UTC",
                             segment);
    }

    private String getRandomElement(String[] array) {
        return array[RandomUtils.secure().randomInt(0,array.length)];
    }

    private void uploadFilesToBlobStorage(File inputFile, List<String> fileNames) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = fileNames.stream()
                .map(fileName -> CompletableFuture.runAsync(() -> {
                    BlobClient blobClient = cvpBlobContainerClient.getBlobClient(fileName);
                    try (InputStream inputStream = new FileInputStream(inputFile)) {
                        blobClient.upload(inputStream, inputFile.length(), true);
                    } catch (IOException e) {
                        LOGGER.error("Failed to upload file: {}", fileName, e);
                    }
                }, executor))
                .toList();
            // Wait for all uploads to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            LOGGER.error("Error occurred during file upload", e);
        }
    }
}
