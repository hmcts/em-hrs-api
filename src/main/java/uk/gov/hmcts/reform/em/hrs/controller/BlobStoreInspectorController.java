package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.storage.BlobSearchResult;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class BlobStoreInspectorController {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreInspectorController.class);

    @Autowired
    HearingRecordingStorage hearingRecordingStorage;

    @GetMapping(value = "/inspect", consumes = MediaType.ALL_VALUE)
    public StorageReport inspect() {

        log.info("BlobStoreInspector Controller");
        return hearingRecordingStorage.getStorageReport();
    }

    @Operation(
        summary = "Get recording file names", description = "Retrieve recording file names for a given folder",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string"))}
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Names of successfully stored recording files"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @GetMapping(value = "/inspect/{source}", params = {"blobName"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<BlobSearchResult> inspectBlob(
        @PathVariable(name = "source") String source,
        @RequestParam(name = "blobName") String blob
        ) {

        log.info("BlobStoreInspector Controller");
        BlobSearchResult searchResult = hearingRecordingStorage.blobSearch(source, blob);
        log.info("BlobStoreInspector searchResult {}", searchResult);

        return ResponseEntity
            .ok()
            .contentType(APPLICATION_JSON)
            .body(searchResult);

    }
}
