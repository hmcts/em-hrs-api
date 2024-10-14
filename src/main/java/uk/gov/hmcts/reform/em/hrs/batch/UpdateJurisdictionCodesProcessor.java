package uk.gov.hmcts.reform.em.hrs.batch;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.exception.BlobCopyException;
import uk.gov.hmcts.reform.em.hrs.exception.BlobNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * This tasklet periodically checks for CSV files in the hmctsmetadata blob container. If it finds one it will download
 * it and then update all the documents with the metadata in the file. After the update has been completed the file
 * is removed from the blob container.
 */
@Component
@ConditionalOnProperty("spring.batch.jurisdictionCodes.enabled")
public class UpdateJurisdictionCodesProcessor implements ItemProcessor<HearingRecordingSegment, HearingRecording> {

    private static final Logger log = LoggerFactory.getLogger(UpdateJurisdictionCodesProcessor.class);

    @Autowired
    @Qualifier("jurisdictionCodesContainerClient")
    private BlobContainerClient blobClient;

    private XSSFWorkbook workbook;

    public HearingRecording process(HearingRecordingSegment hearingRecordingSegment) {

        if (Objects.isNull(workbook)) {
            Optional<BlobClient> workbookBlobClient = loadWorkbookBlobClient();
            if (workbookBlobClient.isPresent()) {
                loadWorkbook(workbookBlobClient.get());
                log.info("loaded workbook");
            } else {
                throw new BlobNotFoundException("blobName", "jurisdictionWorkbook");
            }
        }

        HearingRecording hearingRecording = hearingRecordingSegment.getHearingRecording();
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = sheet.rowIterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            String filename = row.getCell(0).getStringCellValue();

            if (filename.equals(hearingRecordingSegment.getFilename())) {
                Cell jurisdiction = row.getCell(1);
                if (Objects.nonNull(jurisdiction)) {
                    String jurisdictionCode = jurisdiction.getStringCellValue();
                    if (!jurisdictionCode.isBlank()) {
                        hearingRecording.setJurisdictionCode(jurisdictionCode);
                        return hearingRecording;
                    }
                }

                Cell service = row.getCell(2);
                if (Objects.nonNull(service)) {
                    String serviceCode = row.getCell(2).getStringCellValue();
                    if (!serviceCode.isBlank()) {
                        hearingRecording.setServiceCode(serviceCode);
                        return hearingRecording;
                    }
                }
            }
        }
        return hearingRecording;
    }

    private Optional<BlobClient> loadWorkbookBlobClient() {
        return blobClient.listBlobs()
            .stream()
            .map(blobItem -> blobClient.getBlobClient(blobItem.getName()))
            .findAny();
    }


    private void loadWorkbook(BlobClient client) {
        try {
            final File excelFile = File.createTempFile("jurisdictionCodes", ".xslx");
            final String filename = excelFile.getAbsolutePath();

            excelFile.delete();
            client.downloadToFile(filename);

            final InputStream stream = new FileInputStream(filename);

            workbook = new XSSFWorkbook(stream);
        } catch (IOException e) {
            throw new BlobCopyException(e.getMessage());
        }
    }

}
