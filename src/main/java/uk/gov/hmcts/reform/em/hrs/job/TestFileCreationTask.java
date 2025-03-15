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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
    private final String folderName = "audiostream6540/";

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

        File inputFile = loadInputFile();

        List<String> fileNames = new ArrayList<>();

        int totalFiles = 0;
        for (int i = 0; i < testCasesToCreate; i++) {
            String caseRef = "Test" + i * 1000;
            boolean includeLocationCode = i % 3 == 1;
            String baseFileName = folderName + generateBaseFileName(caseRef, includeLocationCode);
            String segment0 = addSegment(baseFileName, "0");
            fileNames.add(segment0);
            totalFiles++;

            // Create an additional file with segment 1 for 10% of the files
            if (i % 10 == 0) {
                String segment1 = addSegment(baseFileName, "1");
                fileNames.add(segment1);
                totalFiles++;
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
        LOGGER.info("Test file creation job took {} ms to create {} files and {} cases",
                stopWatch.getDuration().toMillis(), totalFiles, testCasesToCreate);
        LOGGER.info("Finished {} job", TASK_NAME);
    }

    private File loadInputFile() {
        File inputFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-upload-file.mp4")) {
            if (Objects.isNull(inputStream)) {
                throw new IllegalArgumentException("Invalid input file path");
            }
            inputFile = File.createTempFile("test-upload-file", ".mp4");
            Files.copy(inputStream, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load input file", e);
        }
        return inputFile;
    }

    private String generateBaseFileName(String caseRef, boolean includeLocationCode) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
        String datePart = now.format(formatter);
        String serviceCode = getRandomElement(ttlMapperConfig.getTtlServiceMap().keySet().toArray(new String[0]));
        String locationCodePart = includeLocationCode ? "-" + getRandomElement(LOCATION_CODES) : "";
        return String.format("%s%s-%s_%s-UTC", serviceCode, locationCodePart, caseRef, datePart);
    }

    private String addSegment(String baseFileName, String segment) {
        return String.format("%s_%s.mp4", baseFileName, segment);
    }

    private String getRandomElement(String[] array) {
        return array[RandomUtils.secure().randomInt(0, array.length)];
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
