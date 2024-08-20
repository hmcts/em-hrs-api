package uk.gov.hmcts.reform.em.hrs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.Constants;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@Tag(name = "HearingRecordings Service", description = "Endpoint for managing HearingRecordings.")
public class HearingRecordingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingRecordingController.class);

    private final ShareAndNotifyService shareAndNotifyService;
    private final SegmentDownloadService segmentDownloadService;
    private final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue;
    private final HearingRecordingService hearingRecordingService;

    @Value("${endpoint.deleteCase.enabled}")
    private boolean deleteCaseEndpointEnabled;

    @Autowired
    public HearingRecordingController(
        final ShareAndNotifyService shareAndNotifyService,
        @Qualifier("ingestionQueue") final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue,
        SegmentDownloadService segmentDownloadService, HearingRecordingService hearingRecordingService) {
        this.shareAndNotifyService = shareAndNotifyService;
        this.ingestionQueue = ingestionQueue;
        this.segmentDownloadService = segmentDownloadService;
        this.hearingRecordingService = hearingRecordingService;
    }


    @PostMapping(
        path = "/segments",
        consumes = APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Post hearing recording segment", description = "Save hearing recording segment",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Request accepted for asynchronous processing"),
        @ApiResponse(responseCode = "429", description = "Request rejected - too many pending requests"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<Void> createHearingRecording(@RequestBody final HearingRecordingDto hearingRecordingDto) {
        LOGGER.info(
            """
                posting segment with details:
                rec-ref  {}
                folder   {}
                case-ref {}
                filename {}
                file-ext {}
                segment no {}
                jurisdiction {}
                serviceCode {}
                RecordingSource {}
                sourceBlobUrl {}
                interpreter {}""",
            hearingRecordingDto.getRecordingRef(),
            hearingRecordingDto.getFolder(),
            hearingRecordingDto.getCaseRef(),
            hearingRecordingDto.getFilename(),
            hearingRecordingDto.getFilenameExtension(),
            hearingRecordingDto.getSegment(),
            hearingRecordingDto.getJurisdictionCode(),
            hearingRecordingDto.getServiceCode(),
            hearingRecordingDto.getRecordingSource(),
            hearingRecordingDto.getSourceBlobUrl(),
            hearingRecordingDto.getInterpreter()
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
    @Operation(summary = "Create permissions record", description = "Create permissions record to the specified "
        + "hearing recording and notify user with the link to the resource via email",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return the location of the resource being granted "
            + "access to (the download link)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
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

    @Operation(summary = "Get hearing recording file",
        description = "Return hearing recording file from the specified folder",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Return the requested hearing recording segment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}
    )
    public ResponseEntity<Void> getSegmentBinary(@PathVariable("recordingId") UUID recordingId,
                                           @PathVariable("segment") Integer segmentNo,
                                           @RequestHeader(Constants.AUTHORIZATION) final String userToken,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        return this.downloadWrapper(
            recordingId,
            () ->
                segmentDownloadService
                    .fetchSegmentByRecordingIdAndSegmentNumber(recordingId, segmentNo, userToken, false),
            request,
            response
        );

    }

    @GetMapping(path = {"/hearing-recordings/{recordingId}/file/{fileName}",
        "/hearing-recordings/{recordingId}/file/{folderName}/{fileName}"},
        produces = APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get hearing recording file",
        description = "Return hearing recording file",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Return the requested hearing recording"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}
    )
    public ResponseEntity<Void> getSegmentBinaryByFileName(
        @PathVariable("recordingId") UUID recordingId,
        @PathVariable(value = "folderName", required = false) String folderName,
        @PathVariable("fileName") String fileName,
        HttpServletRequest request,
        HttpServletResponse response) {
        LOGGER.info("recordingId:{}, fileName:{}", recordingId, fileName);
        var fileNameDecoded = folderName == null ? fileName : folderName + File.separator + fileName;
        return this.downloadWrapper(
            recordingId,
            () ->
                segmentDownloadService
                    .fetchSegmentByRecordingIdAndFileName(recordingId, fileNameDecoded),
            request,
            response
        );
    }

    @GetMapping(
        path = {"/hearing-recordings/{recordingId}/file/{fileName}/sharee",
            "/hearing-recordings/{recordingId}/file/{folderName}/{fileName}/sharee"},
        produces = APPLICATION_OCTET_STREAM_VALUE
    )
    @Operation(summary = "Get hearing recording file",
        description = "Return hearing recording file from the specified folder",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(
        value = {@ApiResponse(responseCode = "200", description = "Return the requested hearing recording segment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}
    )
    public ResponseEntity<Void> getSegmentBinaryForShareeByFileName(
        @PathVariable("recordingId") UUID recordingId,
        @PathVariable(value = "folderName", required = false) String folderName,
        @PathVariable("fileName") String fileName,
        @RequestHeader(Constants.AUTHORIZATION) final String userToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        var fileNameDecoded = folderName == null ? fileName : folderName + File.separator + fileName;

        return this.downloadWrapper(
            recordingId,
            () ->
                segmentDownloadService
                    .fetchSegmentByRecordingIdAndFileNameForSharee(recordingId, fileNameDecoded, userToken),
            request,
            response
        );
    }

    @GetMapping(
        path = "/hearing-recordings/{recordingId}/segments/{segment}/sharee",
        produces = APPLICATION_OCTET_STREAM_VALUE
    )
    @Operation(summary = "Get hearing recording file",
        description = "Return hearing recording file from the specified folder",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(
        value = {@ApiResponse(responseCode = "200", description = "Return the requested hearing recording segment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}
    )
    public ResponseEntity<Void> getSegmentBinaryForSharee(
        @PathVariable("recordingId") UUID recordingId,
        @PathVariable("segment") Integer segmentNo,
        @RequestHeader(Constants.AUTHORIZATION) final String userToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return this.downloadWrapper(
            recordingId,
            () ->
                segmentDownloadService
                    .fetchSegmentByRecordingIdAndSegmentNumber(recordingId, segmentNo, userToken, true),
            request,
            response
        );
    }

    @DeleteMapping(
        path = "/delete",
        produces = APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Delete hearing recordings",
        description = "Delete hearing recordings for a given list of CCD case IDs",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "Authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string"))})
    @ApiResponses(
        value = {@ApiResponse(responseCode = "204", description = "Successful deletion"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")}
    )
    public ResponseEntity<Long> deleteCaseHearingRecordings(@RequestBody final List<Long> ccdCaseIds) {
        if (!deleteCaseEndpointEnabled) {
            return ResponseEntity.notFound().build();
        }
        hearingRecordingService.deleteCaseHearingRecordings(ccdCaseIds);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> downloadWrapper(
        UUID recordingId,
        Supplier<HearingRecordingSegment> func,
        HttpServletRequest request,
        HttpServletResponse response) {
        try {
            HearingRecordingSegment segment = func.get();
            segmentDownloadService.download(segment, request, response);
        } catch (AccessDeniedException e) {
            LOGGER.warn(
                "User does not have permission to download recording {}",
                e.getMessage()
            );
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (UncheckedIOException | IOException e) {
            LOGGER.warn(
                "IOException streaming response for recording ID: {} IOException message: {}",
                recordingId, e.getMessage()
            );//Exceptions are thrown during partial requests from front door (it throws client abort)
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
