package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "HearingRecordings Folder Service", description = "Endpoint for managing HearingRecordings Folder.")
public class FolderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderController.class);

    private final FolderService folderService;


    @Autowired
    public FolderController(final FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping(
        path = "/folders/{name}",
        produces = APPLICATION_JSON_VALUE
    )
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
    public ResponseEntity<RecordingFilenameDto> getFilenames(@PathVariable("name") final String folderName) {
        var recordingFilenameDto = new RecordingFilenameDto(
            folderName,
            folderService.getStoredFiles(folderName)
        );

        LOGGER.debug("Under folder {} Completed Filenames: {} ", recordingFilenameDto.getFolderName(), recordingFilenameDto.getFilenames());
        return ResponseEntity
            .ok()
            .contentType(APPLICATION_JSON)
            .body(recordingFilenameDto);
    }


}
