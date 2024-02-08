package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.componenttests.AbstractBaseTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.SegmentDownloadException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.AuditEntryService;
import uk.gov.hmcts.reform.em.hrs.service.Constants;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.INGESTION_QUEUE_SIZE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.convertObjectToJsonString;

class HearingRecordingControllerTest extends AbstractBaseTest {

    @MockBean
    HearingRecordingSegmentRepository segmentRepository;

    @MockBean
    private ShareAndNotifyService shareAndNotifyService;

    @MockBean
    private SegmentDownloadService segmentDownloadService;

    @Autowired
    @Qualifier("ingestionQueue")
    private LinkedBlockingQueue<HearingRecordingDto> ingestionQueue;

    @MockBean
    private AuditEntryService auditEntryService;

    @MockBean
    private HearingRecordingSegmentAuditEntry hearingRecordingSegmentAuditEntry;

    @Test
    void testShouldGrantShareeDownloadAccessToHearingRecording() throws Exception {
        final String path = "/sharees";
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS))
            .id(CCD_CASE_ID)
            .build();
        CallbackRequest callbackRequest = CallbackRequest.builder().caseDetails(caseDetails).build();

        doNothing().when(shareAndNotifyService).shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);

        mockMvc.perform(post(path)
                            .content(convertObjectToJsonString(callbackRequest))
                            .contentType(APPLICATION_JSON_VALUE)
                            .header(Constants.AUTHORIZATION, AUTHORIZATION_TOKEN)
                            .header("ServiceAuthorization", SERVICE_AUTHORIZATION_TOKEN))
            .andExpect(status().isOk());

        verify(shareAndNotifyService, times(1))
            .shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);
    }

    @Test
    void testShouldNotExceedOneSecond() throws Exception {
        final String path = "/segments";
        final Instant start = Instant.now(Clock.systemDefaultZone());

        mockMvc.perform(post(path)
                            .content(convertObjectToJsonString(HEARING_RECORDING_DTO))
                            .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andReturn();

        final Instant end = Instant.now(Clock.systemDefaultZone());

        assertThat(Duration.between(start, end)).isLessThanOrEqualTo(Duration.ofSeconds(1L));
    }

    @Test
    void testShouldReturnRequestAccepted() throws Exception {
        final String path = "/segments";
        ingestionQueue.clear();

        mockMvc.perform(post(path)
                            .content(convertObjectToJsonString(HEARING_RECORDING_DTO))
                            .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andReturn();
    }

    @Test
    void testShouldReturnRequestAcceptedForVhFile() throws Exception {
        final String path = "/segments";
        ingestionQueue.clear();

        mockMvc.perform(post(path)
                            .content(convertObjectToJsonString(VH_HEARING_RECORDING_DTO))
                            .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andReturn();
    }

    @Test
    void testShouldReturnTooManyRequests() throws Exception {
        final String path = "/segments";
        clogJobQueue();

        mockMvc.perform(post(path)
                            .content(convertObjectToJsonString(HEARING_RECORDING_DTO))
                            .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isTooManyRequests())
            .andReturn();
    }

    @Test
    void testShouldDownloadSegment() throws Exception {
        UUID recordingId = UUID.randomUUID();
        doNothing().when(segmentDownloadService)
            .download(any(HearingRecordingSegment.class), any(HttpServletRequest.class),
                      any(HttpServletResponse.class));
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService)
            .createAndSaveEntry(any(HearingRecordingSegment.class), eq(AuditActions.USER_DOWNLOAD_OK));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testShouldHandleSegmentDownloadException() throws Exception {
        UUID recordingId = UUID.randomUUID();
        HearingRecordingSegment segment = new HearingRecordingSegment();
        doReturn(segment).when(segmentDownloadService)
                .fetchSegmentByRecordingIdAndSegmentNumber(any(UUID.class), any(Integer.class),
                                                      eq(TestUtil.AUTHORIZATION_TOKEN), any(boolean.class));
        doThrow(new SegmentDownloadException("failed download"))
            .when(segmentDownloadService)
            .download(any(HearingRecordingSegment.class), any(HttpServletRequest.class),
                      any(HttpServletResponse.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    @Test
    void testShouldHandleSegmentFetchException() throws Exception {
        UUID recordingId = UUID.randomUUID();
        doThrow(RuntimeException.class).when(segmentDownloadService)
            .fetchSegmentByRecordingIdAndSegmentNumber(any(UUID.class), any(Integer.class),
                                                       eq(TestUtil.AUTHORIZATION_TOKEN), any(boolean.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    @Test
    void testShouldDownloadSegmentForSharee() throws Exception {
        UUID recordingId = UUID.randomUUID();
        doNothing().when(segmentDownloadService)
            .download(any(HearingRecordingSegment.class), any(HttpServletRequest.class),
                      any(HttpServletResponse.class));
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService)
            .createAndSaveEntry(any(HearingRecordingSegment.class), eq(AuditActions.USER_DOWNLOAD_OK));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d/sharee", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testShouldHandleSegmentDownloadExceptionForSharee() throws Exception {
        UUID recordingId = UUID.randomUUID();
        HearingRecordingSegment segment = new HearingRecordingSegment();
        doReturn(segment).when(segmentDownloadService)
            .fetchSegmentByRecordingIdAndSegmentNumber(any(UUID.class), any(Integer.class),
                                                       eq(TestUtil.AUTHORIZATION_TOKEN), any(boolean.class));
        doThrow(IOException.class)
            .when(segmentDownloadService)
            .download(any(HearingRecordingSegment.class), any(HttpServletRequest.class),
                      any(HttpServletResponse.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d/sharee", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testShouldHandleSegmentFetchExceptionForSharee() throws Exception {
        UUID recordingId = UUID.randomUUID();
        doThrow(AccessDeniedException.class).when(segmentDownloadService)
            .fetchSegmentByRecordingIdAndSegmentNumber(any(UUID.class), any(Integer.class),
                                                       eq(TestUtil.AUTHORIZATION_TOKEN), any(boolean.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d/sharee", recordingId, 0))
                            .header(Constants.AUTHORIZATION, TestUtil.AUTHORIZATION_TOKEN))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    private void clogJobQueue() {
        IntStream.rangeClosed(1, INGESTION_QUEUE_SIZE + 300)
            .forEach(x -> {
                final HearingRecordingDto dto = HearingRecordingDto.builder()
                    .caseRef("cr" + x)
                    .build();
                ingestionQueue.offer(dto);
            });
    }
}
