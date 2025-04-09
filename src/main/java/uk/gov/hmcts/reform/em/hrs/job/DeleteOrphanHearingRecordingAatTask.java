package uk.gov.hmcts.reform.em.hrs.job;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DeleteOrphanHearingRecordingAatTask implements Runnable {

    private static final String TASK_NAME = "orphan-aat-orpahn-metadata";
    private static final Logger logger = getLogger(DeleteOrphanHearingRecordingAatTask.class);
    private final BlobContainerClient blobClient;

    @Value("${scheduling.task.jurisdiction-codes.batch-size}")
    private int batchSize;

    @Value("${scheduling.task.jurisdiction-codes.thread-limit}")
    private int defaultThreadLimit;

    private final HearingRecordingService hearingRecordingService;


    public DeleteOrphanHearingRecordingAatTask(
            @Qualifier("jurisdictionCodesContainerClient") BlobContainerClient blobClient,
                                       HearingRecordingService hearingRecordingService) {
        this.blobClient = blobClient;
        this.hearingRecordingService = hearingRecordingService;
    }

    @Override
    public void run() {

        logger.info("Started {} job", TASK_NAME);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Optional<BlobClient> csvBlobClient = loadWorkbookBlobClient();
        if (csvBlobClient.isEmpty()) {
            logger.info("No files present for processing");
            return;
        }

        try (ExecutorService executorService = Executors.newFixedThreadPool(defaultThreadLimit);
             XSSFWorkbook workbook = loadWorkbook(csvBlobClient.get())) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            List<String> unProcessedRecords = new ArrayList<>();

            for (Row row : sheet) {
                unProcessedRecords.add(getStringCellValue(row.getCell(0)));
            }

            logger.info("Number of rows in unProcessedRecords: {}", unProcessedRecords.size());

            List<List<String>> batches = Lists.partition(unProcessedRecords, batchSize);
            logger.info("Number of batches created: {}", batches.size());

            for (List<String> batch : batches) {
                executorService.submit(() -> processBatch(batch));
            }

        } catch (IOException e) {
            logger.info("Encountered error deleting orphan HRS metadata: {}", e.getMessage());
        }

        csvBlobClient.get().delete();
        stopWatch.stop();

        logger.info("Deletion job for orphan HRS metadata took {} ms", stopWatch.getDuration().toMillis());
        logger.info("Finished {} job", TASK_NAME);
    }

    private void processBatch(List<String> batch) {
        List<Long> caseIds = batch.stream()
                .map(Long::valueOf)
                .toList();

        hearingRecordingService.deleteCaseHearingRecordings(caseIds);
    }

    private String getStringCellValue(Cell cell) {
        return Objects.isNull(cell) || cell.getStringCellValue().isBlank() ? null : cell.getStringCellValue();
    }

    private Optional<BlobClient> loadWorkbookBlobClient() {
        return blobClient.listBlobs()
                .stream()
                .map(blobItem -> blobClient.getBlobClient(blobItem.getName()))
                .findAny();
    }

    private XSSFWorkbook loadWorkbook(BlobClient client) throws IOException {
        File spreadsheetFile = null;
        try {
            spreadsheetFile = File.createTempFile("jurisdictionCodes", ".xslx");
            final String filename = spreadsheetFile.getAbsolutePath();

            Files.deleteIfExists(spreadsheetFile.toPath());
            client.downloadToFile(filename);

            try (InputStream stream = new FileInputStream(filename)) {
                return new XSSFWorkbook(stream);
            }
        } catch (IOException e) {
            throw new BlobCopyException(e.getMessage());
        } finally {
            if (Objects.nonNull(spreadsheetFile) && spreadsheetFile.exists()) {
                Files.deleteIfExists(spreadsheetFile.toPath());
            }
        }
    }
}
