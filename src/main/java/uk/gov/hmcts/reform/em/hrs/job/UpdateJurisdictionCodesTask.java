package uk.gov.hmcts.reform.em.hrs.job;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class UpdateJurisdictionCodesTask  implements Runnable {

    private static final String TASK_NAME = "jurisdiction-codes";
    private static final Logger logger = getLogger(UpdateJurisdictionCodesTask.class);
    private final BlobContainerClient blobClient;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${scheduling.task.jurisdiction-codes.batch-size}")
    private int batchSize;

    @Value("${scheduling.task.jurisdiction-codes.thread-limit}")
    private int defaultThreadLimit;

    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final HearingRecordingService hearingRecordingService;

    public UpdateJurisdictionCodesTask(@Qualifier("jurisdictionCodesContainerClient") BlobContainerClient blobClient,
                                       NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                       CcdDataStoreApiClient ccdDataStoreApiClient,
                                       HearingRecordingService hearingRecordingService) {
        this.blobClient = blobClient;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
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

            List<UpdateRecordingRecord> unProcessedRecords = new ArrayList<>();

            for (Row row : sheet) {
                UpdateRecordingRecord updateRecordingRecord = new UpdateRecordingRecord(
                    getStringCellValue(row.getCell(0)),
                    getStringCellValue(row.getCell(1)),
                    getStringCellValue(row.getCell(2))
                );
                unProcessedRecords.add(updateRecordingRecord);
            }
            logger.info("Number of rows in unProcessedRecords: {}", unProcessedRecords.size());

            List<List<UpdateRecordingRecord>> batches = Lists.partition(unProcessedRecords, batchSize);
            logger.info("Number of batches created: {}", batches.size()); //300 batches

            Set<Long> ccdCaseIds = ConcurrentHashMap.newKeySet();
            for (List<UpdateRecordingRecord> batch : batches) {
                executorService.submit(() -> processBatch(batch, ccdCaseIds));
            }
        } catch (IOException e) {
            logger.info("Encountered error updating jurisdiction codes: {}", e.getMessage());
        }

        csvBlobClient.get().delete();
        stopWatch.stop();

        logger.info("Update job for jurisdiction codes took {} ms", stopWatch.getDuration().toMillis());
        logger.info("Finished {} job", TASK_NAME);
    }

    private void processBatch(List<UpdateRecordingRecord> batch, Set<Long> ccdCaseIds) {
        for (UpdateRecordingRecord recordingRecord : batch) {
            Long ccdCaseId = hearingRecordingService.findCcdCaseIdByFilename(recordingRecord.filename);
            if (Objects.nonNull(ccdCaseId) && ccdCaseIds.add(ccdCaseId)) {
                updateCaseAndHrsMetaData(recordingRecord, ccdCaseId);
            }
        }
    }

    private void updateCaseAndHrsMetaData(UpdateRecordingRecord recordingRecord, Long ccdCaseId) {

        String filename = recordingRecord.filename;
        try {
            ccdDataStoreApiClient.updateCaseWithCodes(
                ccdCaseId, recordingRecord.jurisdictionCode, recordingRecord.serviceCode);
            logger.info("CCD updated for ccd case id: {}", ccdCaseId);
            updateHrsMetaData(recordingRecord);
            logger.info("HRS DB updated for ccd case id: {}", ccdCaseId);
        } catch (Exception e) {
            logger.info("Failed to update case with jurisdiction and service codes for filename: {}",
                         filename, e);
        } finally {
            logger.info("End updateCaseAndHrsMetaData for {}", recordingRecord.filename);
        }
    }

    private String getStringCellValue(Cell cell) {
        return Objects.isNull(cell) || cell.getStringCellValue().isBlank() ? null : cell.getStringCellValue();
    }

    private void updateHrsMetaData(UpdateRecordingRecord updateRecordingRecord) {
        // This maps the object's properties to the named parameters in the SQL
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(updateRecordingRecord);
        namedParameterJdbcTemplate.update(
            "UPDATE hearing_recording "
                + "SET jurisdiction_code = :jurisdictionCode, service_code = :serviceCode "
                + "WHERE id = (SELECT hearing_recording_id "
                + "FROM hearing_recording_segment "
                + "WHERE filename = :filename)", params);

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

    private record UpdateRecordingRecord(String filename, String jurisdictionCode, String serviceCode){}

}
