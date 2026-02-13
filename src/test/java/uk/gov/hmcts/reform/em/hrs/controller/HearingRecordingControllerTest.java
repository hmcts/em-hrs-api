package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingRecordingControllerTest {

    @Mock
    private ShareAndNotifyService shareAndNotifyService;

    @Mock
    private SegmentDownloadService segmentDownloadService;

    @Mock
    private LinkedBlockingQueue<HearingRecordingDto> ingestionQueue;

    @Mock
    private HearingRecordingService hearingRecordingService;

    @InjectMocks
    private HearingRecordingController hearingRecordingController;

    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final String AUTH_TOKEN = "Bearer token";

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void createHearingRecordingShouldReturnAcceptedWhenQueueOfferSucceeds() {
        HearingRecordingDto dto = HearingRecordingDto.builder().recordingRef("ref").build();
        when(ingestionQueue.offer(any(HearingRecordingDto.class))).thenReturn(true);

        ResponseEntity<Void> response = hearingRecordingController.createHearingRecording(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(dto.getUrlDomain()).isNotNull();
    }

    @Test
    void createHearingRecordingShouldReturnTooManyRequestsWhenQueueOfferFails() {
        HearingRecordingDto dto = HearingRecordingDto.builder().build();
        when(ingestionQueue.offer(any(HearingRecordingDto.class))).thenReturn(false);

        ResponseEntity<Void> response = hearingRecordingController.createHearingRecording(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shareHearingRecordingShouldReturnOk() {
        CaseDetails caseDetails = CaseDetails.builder().id(1L).data(null).build();
        CallbackRequest request = CallbackRequest.builder().caseDetails(caseDetails).build();

        ResponseEntity<Void> response = hearingRecordingController.shareHearingRecording(AUTH_TOKEN, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(shareAndNotifyService).shareAndNotify(eq(1L), any(), eq(AUTH_TOKEN));
    }

    @Test
    void getSegmentBinaryShouldReturnOkOnSuccess() throws IOException {
        HearingRecordingSegment segment = new HearingRecordingSegment();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndSegmentNumber(RECORDING_ID, 1, AUTH_TOKEN, false))
            .thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinary(RECORDING_ID, 1, AUTH_TOKEN, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(segmentDownloadService).download(segment, request, response);
    }

    @Test
    void getSegmentBinaryShouldReturnForbiddenOnAccessDenied() {
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndSegmentNumber(any(), anyInt(), anyString(), anyBoolean()))
            .thenThrow(new AccessDeniedException("Denied"));

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinary(RECORDING_ID, 1, AUTH_TOKEN, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getSegmentBinaryShouldReturnOkWhenIOExceptionOccurs() throws IOException {
        HearingRecordingSegment segment = new HearingRecordingSegment();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndSegmentNumber(any(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(segment);
        doThrow(new IOException("Stream error")).when(segmentDownloadService).download(segment, request, response);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinary(RECORDING_ID, 1, AUTH_TOKEN, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSegmentBinaryShouldReturnOkWhenUncheckedIOExceptionOccurs() throws IOException {
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndSegmentNumber(any(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(segment);
        doThrow(new UncheckedIOException(new IOException())).when(segmentDownloadService).download(any(), any(), any());

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinary(RECORDING_ID, 1, AUTH_TOKEN, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSegmentBinaryByFileNameWithoutFolderShouldReturnOk() {
        String fileName = "file.mp4";
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService.fetchSegmentByRecordingIdAndFileName(RECORDING_ID, fileName)).thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinaryByFileName(RECORDING_ID, null, fileName, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(segmentDownloadService).fetchSegmentByRecordingIdAndFileName(RECORDING_ID, fileName);
    }

    @Test
    void getSegmentBinaryByFileNameWithFolderShouldReturnOk() {
        String fileName = "file.mp4";
        String folderName = "folder";
        String expectedPath = folderName + File.separator + fileName;
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndFileName(RECORDING_ID, expectedPath)).thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinaryByFileName(RECORDING_ID, folderName, fileName, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(segmentDownloadService).fetchSegmentByRecordingIdAndFileName(RECORDING_ID, expectedPath);
    }

    @Test
    void getSegmentBinaryForShareeByFileNameWithFolderShouldReturnOk() {
        String fileName = "file.mp4";
        String folderName = "folder";
        String expectedPath = folderName + File.separator + fileName;
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndFileNameForSharee(RECORDING_ID, expectedPath, AUTH_TOKEN))
            .thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinaryForShareeByFileName(RECORDING_ID, folderName, fileName, AUTH_TOKEN, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSegmentBinaryForShareeByFileNameWithoutFolderShouldReturnOk() {
        String fileName = "file.mp4";
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndFileNameForSharee(RECORDING_ID, fileName, AUTH_TOKEN))
            .thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinaryForShareeByFileName(RECORDING_ID, null, fileName, AUTH_TOKEN, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSegmentBinaryForShareeShouldReturnOk() {
        HearingRecordingSegment segment = new HearingRecordingSegment();
        when(segmentDownloadService
                 .fetchSegmentByRecordingIdAndSegmentNumber(RECORDING_ID, 1, AUTH_TOKEN, true))
            .thenReturn(segment);

        ResponseEntity<Void> result = hearingRecordingController
            .getSegmentBinaryForSharee(RECORDING_ID, 1, AUTH_TOKEN, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteCaseHearingRecordingsShouldReturnNoContentWhenEnabled() {
        ReflectionTestUtils.setField(hearingRecordingController, "deleteCaseEndpointEnabled", true);
        List<Long> ids = List.of(1L);

        ResponseEntity<Void> response = hearingRecordingController.deleteCaseHearingRecordings(ids);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(hearingRecordingService).deleteCaseHearingRecordings(ids);
    }

    @Test
    void deleteCaseHearingRecordingsShouldReturnForbiddenWhenDisabled() {
        ReflectionTestUtils.setField(hearingRecordingController, "deleteCaseEndpointEnabled", false);
        List<Long> ids = List.of(1L);

        ResponseEntity<Void> response = hearingRecordingController.deleteCaseHearingRecordings(ids);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(hearingRecordingService);
    }
}
