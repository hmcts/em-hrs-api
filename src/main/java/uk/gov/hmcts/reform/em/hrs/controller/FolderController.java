package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
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
    @ResponseBody
    @ApiOperation(value = "Get recording file names", notes = "Retrieve recording file names for a given folder")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Names of successfully stored recording files"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<RecordingFilenameDto> getFilenames(@PathVariable("name") final String folderName) {
        var recordingFilenameDto = new RecordingFilenameDto(
            folderName,
            folderService.getStoredFiles(folderName)
        );

        LOGGER.info("Under folder {} Completed Filenames: {} ", folderName, recordingFilenameDto.getFilenames());
        return ResponseEntity
            .ok()
            .contentType(APPLICATION_JSON)
            .body(recordingFilenameDto);
    }


}
