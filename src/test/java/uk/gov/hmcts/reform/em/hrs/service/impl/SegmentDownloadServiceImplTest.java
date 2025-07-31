package uk.gov.hmcts.reform.em.hrs.service.impl;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidRangeRequestException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.Constants;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;
import uk.gov.hmcts.reform.em.hrs.storage.BlobInfo;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegmentDownloadServiceImplTest {

    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final UUID SEGMENT_ID = UUID.randomUUID();
    private static final String FILENAME = "rec_123_seg_0.mp4";
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_TOKEN = "user-token";
    private static final long CCD_CASE_ID = 1234567890L;
    private static final int LINK_VALIDITY_HOURS = 72;

    @Mock
    private HearingRecordingSegmentRepository segmentRepository;
    @Mock
    private BlobstoreClient blobstoreClient;
    @Mock
    private AuditEntryService auditEntryService;
    @Mock
    private ShareesRepository shareesRepository;
    @Mock
    private SecurityService securityService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletOutputStream servletOutputStream;

    private SegmentDownloadServiceImpl segmentDownloadService;
    private HearingRecordingSegment segment;

    @BeforeEach
    void setUp() {
        segmentDownloadService = new SegmentDownloadServiceImpl(
            segmentRepository,
            blobstoreClient,
            auditEntryService,
            shareesRepository,
            securityService,
            LINK_VALIDITY_HOURS
        );

        HearingRecording hearingRecording = new HearingRecording();
        hearingRecording.setId(RECORDING_ID);
        hearingRecording.setCcdCaseId(CCD_CASE_ID);
        hearingRecording.setHearingSource(HearingSource.CVP.name());

        segment = new HearingRecordingSegment();
        segment.setId(SEGMENT_ID);
        segment.setFilename(FILENAME);
        segment.setRecordingSegment(0);
        segment.setHearingRecording(hearingRecording);
    }

    @Nested
    @DisplayName("fetchSegmentByRecordingIdAndSegmentNumber Tests")
    class FetchSegmentBySegmentNumber {

        @Test
        @DisplayName("Should return segment for a non-sharee user")
        void testFetchSegmentForNonSharee() {
            when(segmentRepository.findByHearingRecordingIdAndRecordingSegment(RECORDING_ID, 0))
                .thenReturn(segment);

            HearingRecordingSegment result = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
                RECORDING_ID, 0, USER_TOKEN, false
            );

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SEGMENT_ID);
            verify(securityService, never()).getUserEmail(anyString());
            verify(shareesRepository, never()).findByShareeEmailIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should return segment for a sharee with a valid, non-expired link")
        void testFetchSegmentForShareeWithValidLink() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            List<HearingRecordingSharee> sharees = createShareeList(RECORDING_ID, LocalDateTime.now());
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(sharees);
            when(segmentRepository.findByHearingRecordingIdAndRecordingSegment(RECORDING_ID, 0))
                .thenReturn(segment);

            HearingRecordingSegment result = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
                RECORDING_ID, 0, USER_TOKEN, true
            );

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SEGMENT_ID);
        }

        @Test
        @DisplayName("Should throw ValidationErrorException for a sharee with an expired link")
        void testFetchSegmentForShareeWithExpiredLink() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            LocalDateTime expiredDateTime = LocalDateTime.now().minusHours(LINK_VALIDITY_HOURS + 1);
            List<HearingRecordingSharee> sharees = createShareeList(RECORDING_ID, expiredDateTime);
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(sharees);

            var exception = assertThrows(ValidationErrorException.class, () ->
                segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
                    RECORDING_ID, 0, USER_TOKEN, true
                ));

            assertThat(exception.getData()).containsEntry("error", Constants.SHARED_EXPIRED_LINK_MSG);
            verify(segmentRepository, never()).findByHearingRecordingIdAndRecordingSegment(any(), anyInt());
        }

        @Test
        @DisplayName("Should fetch segment successfully if sharee check passes but no shares are found")
        void testFetchSegmentWhenShareeHasNoShares() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(Collections.emptyList());
            when(segmentRepository.findByHearingRecordingIdAndRecordingSegment(RECORDING_ID, 0))
                .thenReturn(segment);

            HearingRecordingSegment result = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
                RECORDING_ID, 0, USER_TOKEN, true
            );

            assertThat(result).isEqualTo(segment);
            verify(shareesRepository).findByShareeEmailIgnoreCase(USER_EMAIL);
        }
    }

    @Nested
    @DisplayName("fetchSegmentByRecordingIdAndFileNameForSharee Tests")
    class FetchSegmentByFileNameForSharee {
        @Test
        @DisplayName("Should return segment for a sharee with a valid, non-expired link")
        void testFetchSegmentWithValidLink() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            List<HearingRecordingSharee> sharees = createShareeList(RECORDING_ID, LocalDateTime.now());
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(sharees);
            when(segmentRepository.findByHearingRecordingIdAndFilename(RECORDING_ID, FILENAME)).thenReturn(segment);

            HearingRecordingSegment result = segmentDownloadService.fetchSegmentByRecordingIdAndFileNameForSharee(
                RECORDING_ID, FILENAME, USER_TOKEN
            );

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SEGMENT_ID);
        }

        @Test
        @DisplayName("Should throw ValidationErrorException when sharee has no shares for that recording")
        void testShouldThrowExceptionForWrongRecording() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            List<HearingRecordingSharee> sharees = createShareeList(UUID.randomUUID(), LocalDateTime.now());
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(sharees);

            var exception = assertThrows(ValidationErrorException.class, () ->
                segmentDownloadService.fetchSegmentByRecordingIdAndFileNameForSharee(
                    RECORDING_ID, FILENAME, USER_TOKEN
                ));

            assertThat(exception.getData()).containsEntry("error", Constants.SHARED_EXPIRED_LINK_MSG);
        }

        @Test
        @DisplayName("Should throw ValidationErrorException when user has no shares at all")
        void testShouldThrowExceptionWhenNoSharesExist() {
            when(securityService.getUserEmail(USER_TOKEN)).thenReturn(USER_EMAIL);
            when(shareesRepository.findByShareeEmailIgnoreCase(USER_EMAIL)).thenReturn(Collections.emptyList());

            var exception = assertThrows(ValidationErrorException.class, () ->
                segmentDownloadService.fetchSegmentByRecordingIdAndFileNameForSharee(
                    RECORDING_ID, FILENAME, USER_TOKEN
                ));

            assertThat(exception.getData()).containsEntry("error", Constants.NO_SHARED_FILE_FOR_USER);
            verify(segmentRepository, never()).findByHearingRecordingIdAndFilename(any(), anyString());
        }
    }

    @Nested
    @DisplayName("download Tests")
    class Download {
        @BeforeEach
        void downloadSetup() {
            when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
            when(blobstoreClient.fetchBlobInfo(FILENAME, "CVP"))
                .thenReturn(new BlobInfo(2000L, "video/mp4"));
        }

        @Test
        @DisplayName("Should stream full file and set correct headers when no range is requested")
        void testFullDownload() throws IOException {
            when(response.getOutputStream()).thenReturn(servletOutputStream);

            segmentDownloadService.download(segment, request, response);

            verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
            verify(response).setHeader(HttpHeaders.CONTENT_LENGTH, "2000");
            verify(response).setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            verify(blobstoreClient).downloadFile(FILENAME, null, servletOutputStream, "CVP");
            verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);
            verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        }

        @Test
        @DisplayName("Should stream partial file for a valid range request")
        void testRangedDownload() throws IOException {
            when(response.getOutputStream()).thenReturn(servletOutputStream);
            when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=0-1023");

            segmentDownloadService.download(segment, request, response);

            verify(response).setStatus(HttpStatus.PARTIAL_CONTENT.value());
            verify(response).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/2000");
            verify(response).setHeader(HttpHeaders.CONTENT_LENGTH, "1024");

            verify(blobstoreClient).downloadFile(
                Mockito.eq(FILENAME),
                Mockito.argThat(range -> range.getOffset() == 0 && range.getCount() == 1024L),
                Mockito.eq(servletOutputStream),
                Mockito.eq("CVP")
            );
            verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        }

        @Test
        @DisplayName("Should cap range at file size if requested range is too large")
        void testRangedDownloadTooLarge() throws IOException {
            when(response.getOutputStream()).thenReturn(servletOutputStream);
            when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=1000-2999");

            segmentDownloadService.download(segment, request, response);

            verify(response).setStatus(HttpStatus.PARTIAL_CONTENT.value());
            verify(response).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 1000-1999/2000");
            verify(response).setHeader(HttpHeaders.CONTENT_LENGTH, "1000");
            verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        }

        @Test
        @DisplayName("Should throw exception and audit failure for invalid range format")
        void testInvalidRangeFormat() {
            when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=A-Z");

            var exception = assertThrows(InvalidRangeRequestException.class, () ->
                segmentDownloadService.download(segment, request, response));

            assertThat(exception).isNotNull();
            verify(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_FAIL);
            verify(auditEntryService, never()).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        }

        @Test
        @DisplayName("Should not audit success when blob client throws a runtime exception")
        void testDownloadFailsDueToBlobstoreClientException() throws IOException {
            when(response.getOutputStream()).thenReturn(servletOutputStream);
            doThrow(new RuntimeException("Blob store unavailable"))
                .when(blobstoreClient).downloadFile(anyString(), any(), any(), anyString());

            assertThrows(RuntimeException.class, () ->
                segmentDownloadService.download(segment, request, response));

            verify(auditEntryService, times(1))
                .createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);
            verify(auditEntryService, never())
                .createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
            verify(auditEntryService, never())
                .createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_FAIL);
        }
    }

    private List<HearingRecordingSharee> createShareeList(UUID recordingId, LocalDateTime sharedOn) {
        HearingRecordingSharee sharee = new HearingRecordingSharee();
        sharee.setSharedOn(sharedOn);
        sharee.setShareeEmail(USER_EMAIL);

        HearingRecording recording = new HearingRecording();
        recording.setId(recordingId);

        HearingRecordingSegment sharedSegment = new HearingRecordingSegment();
        sharedSegment.setRecordingSegment(0);
        sharedSegment.setFilename(FILENAME);
        recording.setSegments(new HashSet<>(Set.of(sharedSegment)));

        sharee.setHearingRecording(recording);
        return new ArrayList<>(List.of(sharee));
    }
}
