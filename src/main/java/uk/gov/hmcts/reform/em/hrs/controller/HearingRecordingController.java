package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
public class HearingRecordingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingRecordingController.class);


    private final ShareAndNotifyService shareAndNotifyService;
    private final SegmentDownloadService segmentDownloadService;
    private final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue;

    @Autowired
    public HearingRecordingController(
        final ShareAndNotifyService shareAndNotifyService,
        @Qualifier("ingestionQueue") final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue,
        SegmentDownloadService segmentDownloadService
    ) {
        this.shareAndNotifyService = shareAndNotifyService;
        this.ingestionQueue = ingestionQueue;
        this.segmentDownloadService = segmentDownloadService;
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
        LOGGER.info(
            "posting segment with details:\n"
                + "rec-ref  {}\n"
                + "folder   {}\n"
                + "case-ref {}\n"
                + "filename {}\n"
                + "file-ext {}\n"
                + "segment no {}",
            hearingRecordingDto.getRecordingRef(),
            hearingRecordingDto.getFolder(),
            hearingRecordingDto.getCaseRef(),
            hearingRecordingDto.getFilename(),
            hearingRecordingDto.getFilenameExtension(),
            hearingRecordingDto.getSegment()
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

        shareAndNotifyService.shareAndNotify(
            caseDetails.getId(),
            caseDetails.getData(),
            authorisationToken
        );

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
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        try {
            //TODO this should return a 403 if its not in database
            HearingRecordingSegment segment = segmentDownloadService
                .fetchSegmentByRecordingIdAndSegmentNumber(recordingId, segmentNo);
            segmentDownloadService.download(segment, request, response);
        } catch (AccessDeniedException e) {
            LOGGER.warn(
                "User does not have permission to download recording {}",
                e.getMessage()
            );
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            LOGGER.warn(
                "Download Issue possibly client abort {}",
                e.getMessage()
            );//Exceptions are thrown during partial requests from front door (it throws client abort)
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
