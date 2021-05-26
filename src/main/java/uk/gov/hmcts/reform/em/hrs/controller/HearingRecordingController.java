package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.exception.SegmentDownloadException;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;
import uk.gov.hmcts.reform.em.hrs.util.IngestionQueue;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
public class HearingRecordingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingRecordingController.class);

    private final FolderService folderService;
    private final ShareAndNotifyService shareAndNotifyService;
    private final SegmentDownloadService downloadService;
    private final IngestionQueue ingestionQueue;

    @Autowired
    public HearingRecordingController(final FolderService folderService,
                                      final ShareAndNotifyService shareAndNotifyService,
                                      final IngestionQueue ingestionQueue,
                                      SegmentDownloadService downloadService) {
        this.folderService = folderService;
        this.shareAndNotifyService = shareAndNotifyService;
        this.ingestionQueue = ingestionQueue;
        this.downloadService = downloadService;
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
        final RecordingFilenameDto recordingFilenameDto = new RecordingFilenameDto(
            folderName,
            folderService.getStoredFiles(folderName)
        );

        LOGGER.info("returning the filenames under folder {}", folderName);
        return ResponseEntity
            .ok()
            .contentType(APPLICATION_JSON)
            .body(recordingFilenameDto);
    }

    @PostMapping(
        path = "/segments",
        consumes = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @ApiOperation(value = "Post hearing recording segment", notes = "Save hearing recording segment")
    @ApiResponses(value = {
        @ApiResponse(code = 202, message = "Request accepted for asynchronous processing"),
        @ApiResponse(code = 429, message = "Request rejected - too many pending requests"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<Void> createHearingRecording(@RequestBody final HearingRecordingDto hearingRecordingDto) {
        LOGGER.info("received request to create hearing recording with ref {} in folder {}",
                    hearingRecordingDto.getRecordingRef(),
                    hearingRecordingDto.getFolder()
        );

        hearingRecordingDto.setUrlDomain(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        final boolean accepted = ingestionQueue.offer(hearingRecordingDto);

        return accepted ? new ResponseEntity<>(ACCEPTED) : new ResponseEntity<>(TOO_MANY_REQUESTS);
    }

    @PostMapping(
        path = "/sharees",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @ApiOperation(value = "Create permissions record", notes = "Create permissions record to the specified "
        + "hearing recording and notify user with the link to the resource via email")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the location of the resource being granted "
            + "access to (the download link)"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<Void> shareHearingRecording(
        @RequestHeader("authorization") final String authorisationToken,
        @RequestBody final CallbackRequest request) {

        CaseDetails caseDetails = request.getCaseDetails();

        LOGGER.info("received request to share recordings for case ({})", caseDetails.getId());

        shareAndNotifyService.shareAndNotify(caseDetails.getId(),
                                             caseDetails.getData(),
                                             authorisationToken);

        return ResponseEntity.ok().build();
    }

    @GetMapping(
        path = "/hearing-recordings/{recordingId}/segments/{segment}",
        produces = APPLICATION_OCTET_STREAM_VALUE
    )
    @ResponseBody
    @ApiOperation(value = "Get hearing recording file",
        notes = "Return hearing recording file from the specified folder")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return the requested hearing recording segment")})
    public ResponseEntity getSegmentBinary(@PathVariable("recordingId") UUID recordingId,
                                           @PathVariable("segment") Integer segmentNo,
                                           HttpServletResponse response) {

        LOGGER.info("received request to download recording for case ({}) segment ({})", recordingId, segmentNo);

        Map<String, String> segmentDetails = downloadService.getDownloadInfo(recordingId, segmentNo);
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", segmentDetails.get("filename"))
        );
        response.setHeader(HttpHeaders.CONTENT_TYPE, segmentDetails.get("contentType"));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, segmentDetails.get("contentLength"));

        try {
            downloadService.download(segmentDetails.get("filename"), response.getOutputStream());
        } catch (IOException e) {
            throw new SegmentDownloadException(e);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
