package uk.gov.hmcts.reform.em.hrs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class BlobStoreInspectorController {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreInspectorController.class);

    @Autowired
    HearingRecordingStorage hearingRecordingStorage;

    @GetMapping(value = "/inspect", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> inspect() {

        StorageReport report = hearingRecordingStorage.getStorageReport();
        String reportStr = "CVP Count = " + report.cvpItemCount;
        reportStr += " vs HRS Count = " + report.hrsItemCount;
        String htmlResponse = "Blobstores Inspected<p>" + reportStr.replace("\n", "<p>");
        return ok(htmlResponse);
    }
}
