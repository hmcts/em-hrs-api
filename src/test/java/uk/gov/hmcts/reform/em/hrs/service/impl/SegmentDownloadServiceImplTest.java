package uk.gov.hmcts.reform.em.hrs.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {SegmentDownloadServiceImpl.class})
class SegmentDownloadServiceImplTest {

    @MockitoBean
    private HearingRecordingSegmentRepository segmentRepository;

    @MockitoBean
    private BlobstoreClient blobstoreClient;

    @MockitoBean
    private AuditEntryService auditEntryService;

    @MockitoBean
    private HttpServletRequest request;

    @MockitoBean
    private HttpServletResponse response;

    @MockitoBean
    private HearingRecordingSegmentAuditEntry hearingRecordingSegmentAuditEntry;

    @MockitoBean
    private ShareesRepository shareesRepository;

    @MockitoBean
    private SecurityService securityService;

    private HearingRecordingSegment segment;

    private static final UUID SEGMENT_ID = UUID.randomUUID();

    private static final UUID SEGMENT11_ID = UUID.randomUUID();
    private static final String FILE_NAME_11_ID = "audiostream/231-312-312.mp4";
    private static final UUID SEGMENT12_ID = UUID.randomUUID();

    private static final UUID SEGMENT21_ID = UUID.randomUUID();
    private static final UUID SEGMENT22_ID = UUID.randomUUID();

    private static final String FILE_NAME = "XYZ";

    @Autowired
    private SegmentDownloadServiceImpl segmentDownloadService;

    private Enumeration<String> generateEmptyHeaders() {
        // define the headers you want to be returned
        Map<String, String> headers = new HashMap<>();
        return Collections.enumeration(headers.keySet());
    }

    @BeforeEach
    void before() {
        segment = new HearingRecordingSegment();
        segment.setId(SEGMENT_ID);
        segment.setFilename(FILE_NAME);
        segment.setRecordingSegment(0);
        HearingRecording hr = new HearingRecording();
        hr.setCcdCaseId(TestUtil.CCD_CASE_ID);
        hr.setHearingSource(HearingSource.CVP.name());
        segment.setHearingRecording(hr);
    }

    @Test
    void testFetchSegmentByRecordingIdAndSegmentNumber() {
        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndRecordingSegment(SEGMENT_ID, 0);
        doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
        doReturn(null).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
        HearingRecordingSegment returnedSegment = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
            SEGMENT_ID, 0, TestUtil.AUTHORIZATION_TOKEN, false);
        assertEquals(SEGMENT_ID, returnedSegment.getId());
        assertEquals(FILE_NAME, returnedSegment.getFilename());
        assertEquals(TestUtil.CCD_CASE_ID, returnedSegment.getHearingRecording().getCcdCaseId());
    }


    @Test
    void testFetchSegmentByRecordingIdAndFileNameForSharee() {
        segment.setFilename(FILE_NAME_11_ID);
        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndFilename(SEGMENT11_ID, FILE_NAME_11_ID);
        doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
        List<HearingRecordingSharee> hearingRecordingSharees = createHearingRecordingSharees();
        hearingRecordingSharees.stream()
            .forEach(hearingRecordingSharee ->
                         hearingRecordingSharee.getHearingRecording().setId(SEGMENT11_ID));

        doReturn(hearingRecordingSharees).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
        HearingRecordingSegment returnedSegment = segmentDownloadService.fetchSegmentByRecordingIdAndFileNameForSharee(
            SEGMENT11_ID, FILE_NAME_11_ID, TestUtil.AUTHORIZATION_TOKEN);
        assertEquals(SEGMENT_ID, returnedSegment.getId());
        assertEquals(FILE_NAME_11_ID, returnedSegment.getFilename());
        assertEquals(TestUtil.CCD_CASE_ID, returnedSegment.getHearingRecording().getCcdCaseId());
    }

    @Test
    void testFetchSegmentByRecordingIdAndFileNameForShareeThrowsExceptionIfLinkExpired() {
        try {
            segment.setFilename(FILE_NAME_11_ID);
            doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndFilename(
                SEGMENT11_ID,
                FILE_NAME_11_ID
            );
            doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
            List<HearingRecordingSharee> hearingRecordingSharees = createHearingRecordingSharees();
            hearingRecordingSharees.stream()
                .forEach(hearingRecordingSharee -> {
                    hearingRecordingSharee.setSharedOn(LocalDateTime.now().minusHours(73));
                    hearingRecordingSharee.getHearingRecording().setId(SEGMENT11_ID);
                });
            doReturn(hearingRecordingSharees).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
            segmentDownloadService.fetchSegmentByRecordingIdAndFileNameForSharee(
                SEGMENT11_ID, FILE_NAME_11_ID, TestUtil.AUTHORIZATION_TOKEN);
        } catch (ValidationErrorException validationErrorException) {
            Assertions.assertEquals(Constants.SHARED_EXPIRED_LINK_MSG, validationErrorException.getData().get("error"));
        }
    }

    @Test
    void testFetchSegmentByRecordingIdAndFileName() {
        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndFilename(SEGMENT_ID, FILE_NAME);
        HearingRecordingSegment returnedSegment = segmentDownloadService.fetchSegmentByRecordingIdAndFileName(
            SEGMENT_ID, FILE_NAME);
        assertEquals(SEGMENT_ID, returnedSegment.getId());
        assertEquals(FILE_NAME, returnedSegment.getFilename());
        assertEquals(TestUtil.CCD_CASE_ID, returnedSegment.getHearingRecording().getCcdCaseId());
    }

    @Test
    void testFetchSegmentByRecordingIdAndSegmentNumberForNonExpiredLink() {
        segment.setRecordingSegment(1);
        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndRecordingSegment(SEGMENT21_ID, 1);
        doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
        doReturn(createHearingRecordingSharees()).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
        HearingRecordingSegment returnedSegment = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
            SEGMENT21_ID, 1, TestUtil.AUTHORIZATION_TOKEN, true);
        assertEquals(SEGMENT_ID, returnedSegment.getId());
        assertEquals(FILE_NAME, returnedSegment.getFilename());
        assertEquals(TestUtil.CCD_CASE_ID, returnedSegment.getHearingRecording().getCcdCaseId());
    }

    @Test
    void testFetchSegmentByRecordingIdAndSegmentNumberForNonExpiredLinkSharedAgain() {
        List<HearingRecordingSharee> hearingRecordingSharees = createHearingRecordingSharees();
        hearingRecordingSharees.stream().findFirst().get().setSharedOn(LocalDateTime.now().minusHours(74));
        hearingRecordingSharees
            .forEach(hearingRecordingSharee ->
                         hearingRecordingSharee.getHearingRecording().setId(SEGMENT21_ID));
        segment.setRecordingSegment(1);
        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndRecordingSegment(SEGMENT21_ID, 1);
        doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
        doReturn(hearingRecordingSharees).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
        HearingRecordingSegment returnedSegment = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
            SEGMENT21_ID, 1, TestUtil.AUTHORIZATION_TOKEN, true);
        assertEquals(SEGMENT_ID, returnedSegment.getId());
        assertEquals(FILE_NAME, returnedSegment.getFilename());
        assertEquals(TestUtil.CCD_CASE_ID, returnedSegment.getHearingRecording().getCcdCaseId());
    }

    @Test
    void testFetchSegmentByRecordingIdAndSegmentNumberForExpiredLink() {
        try {
            List<HearingRecordingSharee> hearingRecordingSharees = createHearingRecordingSharees();
            hearingRecordingSharees
                .forEach(hearingRecordingSharee ->
                             hearingRecordingSharee.setSharedOn(LocalDateTime.now().minusHours(73)));
            doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndRecordingSegment(SEGMENT21_ID, 1234);
            doReturn(TestUtil.SHARER_EMAIL_ADDRESS).when(securityService).getUserEmail(anyString());
            doReturn(hearingRecordingSharees).when(shareesRepository).findByShareeEmailIgnoreCase(anyString());
            var result = segmentDownloadService.fetchSegmentByRecordingIdAndSegmentNumber(
                SEGMENT21_ID, 1234, TestUtil.AUTHORIZATION_TOKEN, true);
            assertEquals(this.segment, result);
        } catch (ValidationErrorException validationErrorException) {
            assertEquals(Constants.SHARED_EXPIRED_LINK_MSG, validationErrorException.getData().get("error"));
        }
    }

    @Test
    void testDownloadForCVP() throws IOException {
        doReturn(segment).when(segmentRepository).findByFilename(segment.getFilename());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(1000L, null));
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);

        doNothing().when(blobstoreClient).downloadFile(segment.getFilename(), null, null, "CVP");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());

        segmentDownloadService.download(segment, request, response);

        verify(blobstoreClient, times(1)).downloadFile(segment.getFilename(), null, null, "CVP");
    }

    @Test
    void testDownloadForVH() throws IOException {
        segment.getHearingRecording().setHearingSource(HearingSource.VH.name());
        doReturn(segment).when(segmentRepository).findByFilename(segment.getFilename());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(1000L, null));
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);

        doNothing().when(blobstoreClient).downloadFile(segment.getFilename(), null, null, "VH");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());

        segmentDownloadService.download(segment, request, response);

        verify(blobstoreClient, times(1)).downloadFile(segment.getFilename(), null, null, "VH");
    }

    @Test
    void loadsRangedBlobInvalidRangeHeaderStart() {
        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=A-Z");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(1000L, null));
        assertThrows(InvalidRangeRequestException.class, () -> {
            segmentDownloadService.download(segment, request, response);
        });
    }

    @Test
    void loadsRangedBlobInvalidRangeHeaderStartGreaterThanEnd() {
        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=1023-0");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(1000L, null));
        assertThrows(InvalidRangeRequestException.class, () -> {
            segmentDownloadService.download(segment, request, response);
        });
    }

    @Test
    void loadsRangedBlobTooLargeRangeHeader() throws IOException {
        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=0-1023");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(1000L, null));
        segmentDownloadService.download(segment, request, response);

        Mockito.verify(response, Mockito.times(1)).setStatus(HttpStatus.PARTIAL_CONTENT.value());
        //if the blob range is larger than the file, then the whole file is returned
        //should the status be partial content or not? given it is the complete content vs was a range request...
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-999/1000");
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_LENGTH, "1000");
    }

    @Test
    void loadsRangedBlobValidRangeHeader() throws IOException {
        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=0-1023");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
        when(blobstoreClient.fetchBlobInfo(any(), any())).thenReturn(new BlobInfo(2000L, null));
        segmentDownloadService.download(segment, request, response);
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpStatus.PARTIAL_CONTENT.value());
        Mockito.verify(response, Mockito.times(1))
            .setHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/2000");
        Mockito.verify(response, Mockito.times(1))
            .setHeader(HttpHeaders.CONTENT_LENGTH, "1024");
    }

    private List<HearingRecordingSharee> createHearingRecordingSharees() {

        HearingRecordingSharee hearingRecordingSharee1 = new HearingRecordingSharee();
        HearingRecording hearingRecording1 = new HearingRecording();
        hearingRecordingSharee1.setHearingRecording(hearingRecording1);
        hearingRecordingSharee1.setSharedOn(LocalDateTime.now().plusMinutes(12));

        HearingRecordingSegment segment11 = new HearingRecordingSegment();
        segment11.setId(SEGMENT11_ID);
        segment11.setFilename(FILE_NAME_11_ID);
        segment11.setRecordingSegment(0);
        HearingRecordingSegment segment12 = new HearingRecordingSegment();
        segment12.setId(SEGMENT12_ID);
        segment12.setRecordingSegment(1);
        segment12.setCreatedOn(LocalDateTime.now());

        Set<HearingRecordingSegment> segment1Set = new HashSet<>();
        segment1Set.add(segment11);
        segment1Set.add(segment12);
        hearingRecording1.setSegments(segment1Set);
        hearingRecording1.setId(SEGMENT11_ID);

        HearingRecordingSharee hearingRecordingSharee2 = new HearingRecordingSharee();
        HearingRecording hearingRecording2 = new HearingRecording();
        hearingRecordingSharee2.setHearingRecording(hearingRecording2);
        hearingRecordingSharee2.setSharedOn(LocalDateTime.now().plusMinutes(12));

        HearingRecordingSegment segment21 = new HearingRecordingSegment();
        segment21.setId(SEGMENT21_ID);
        segment21.setRecordingSegment(1);
        HearingRecordingSegment segment22 = new HearingRecordingSegment();
        segment22.setId(SEGMENT22_ID);
        segment22.setRecordingSegment(0);

        Set<HearingRecordingSegment> segment2Set = new HashSet<>();
        segment2Set.add(segment21);
        segment2Set.add(segment22);
        hearingRecording2.setSegments(segment2Set);
        hearingRecording2.setId(SEGMENT21_ID);

        List<HearingRecordingSharee> hearingRecordingSharees = new ArrayList<>();
        hearingRecordingSharees.add(hearingRecordingSharee1);
        hearingRecordingSharees.add(hearingRecordingSharee2);

        return hearingRecordingSharees;
    }
}
