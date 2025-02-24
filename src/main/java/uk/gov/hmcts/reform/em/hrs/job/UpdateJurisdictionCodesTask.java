package uk.gov.hmcts.reform.em.hrs.job;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.exception.BlobNotFoundException;
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

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.jurisdiction-codes.enabled")
public class UpdateJurisdictionCodesTask {

    private static final String TASK_NAME = "jurisdiction-codes";
    private static final Logger logger = getLogger(UpdateJurisdictionCodesTask.class);
    private final BlobContainerClient blobClient;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final int batchSize = 10;
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


    @Scheduled(cron = "${scheduling.task.jurisdiction-codes.cron}", zone = "Europe/London")
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Optional<BlobClient> csvBlobClient = loadWorkbookBlobClient();
        if (csvBlobClient.isEmpty()) {
            throw new BlobNotFoundException("blobName", "jurisdictionWorkbook");
        }

        List<UpdateRecordingRecord> records = new ArrayList<>();

        try (XSSFWorkbook workbook = loadWorkbook(csvBlobClient.get())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                UpdateRecordingRecord updateRecordingRecord = new UpdateRecordingRecord(
                    getStringCellValue(row.getCell(0)),
                    getStringCellValue(row.getCell(1)),
                    getStringCellValue(row.getCell(2))
                );
                if (!updateCase(updateRecordingRecord)) {
                    continue;
                }

                records.add(updateRecordingRecord);

                if (records.size() >= batchSize) {
                    batchUpdate(records);
                    records.clear();
                }
            }

            if (!records.isEmpty()) {
                batchUpdate(records);
            }
        } catch (IOException e) {
            logger.info("Encountered error updating jurisdiction codes: {}", e.getMessage());
        }
        csvBlobClient.get().delete();
        stopWatch.stop();
        logger.info("Update job for jurisdiction codes took {} ms", stopWatch.getDuration().toMillis());
        logger.info("Finished {} job", TASK_NAME);
    }

    private boolean updateCase(UpdateRecordingRecord recordingRecord) {
        String filename = recordingRecord.filename;
        Long ccdCaseId = hearingRecordingService.findCcdCaseIdByFilename(filename);
        if (Objects.isNull(ccdCaseId)) {
            logger.info("Failed to find ccd case id for filename: {}", filename);
            return false;
        }
        try {
            ccdDataStoreApiClient.updateCaseWithCodes(
                ccdCaseId, recordingRecord.jurisdictionCode, recordingRecord.serviceCode);
        } catch (Exception e) {
            logger.info("Failed to update case with jurisdiction and service codes for filename: {}",
                         filename, e);
            return false;
        }
        return true;
    }

    private String getStringCellValue(Cell cell) {
        return Objects.isNull(cell) || cell.getStringCellValue().isBlank() ? null : cell.getStringCellValue();
    }

    private void batchUpdate(List<UpdateRecordingRecord> records) {
        namedParameterJdbcTemplate.batchUpdate("UPDATE hearing_recording "
                                      + "SET jurisdiction_code = :jurisdictionCode, service_code = :serviceCode "
                                      + "WHERE id = (" + "SELECT hearing_recording_id "
                                      + "FROM hearing_recording_segment "
                                      + "WHERE filename = :filename)", SqlParameterSourceUtils.createBatch(records));
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
