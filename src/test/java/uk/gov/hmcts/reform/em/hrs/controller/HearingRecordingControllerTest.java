package uk.gov.hmcts.reform.em.hrs.controller;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.componenttests.AbstractBaseTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.SegmentDownloadException;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentDownloadService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;
import uk.gov.hmcts.reform.em.hrs.util.IngestionQueue;

import java.io.OutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.inject.Inject;

import static java.util.Collections.emptySet;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.INGESTION_QUEUE_SIZE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.convertObjectToJsonString;

class HearingRecordingControllerTest extends AbstractBaseTest {
    private static final String TEST_FOLDER = "folder-1";
    @MockBean
    private FolderService folderService;

    @MockBean
    private ShareAndNotifyService shareAndNotifyService;

    @MockBean
    private SegmentDownloadService downloadService;

    @Inject
    private IngestionQueue ingestionQueue;

    @Test
    void testWhenRequestedFolderDoesNotExistOrIsEmpty() throws Exception {
        final String path = "/folders/" + TEST_FOLDER;
        doReturn(emptySet()).when(folderService).getStoredFiles(TEST_FOLDER);

        final MvcResult mvcResult = mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andReturn();

        final String content = mvcResult.getResponse().getContentAsString();

        assertThatJson(content)
            .when(Option.IGNORING_ARRAY_ORDER)
            .and(
                x -> x.node("folder-name").isPresent().isEqualTo(TEST_FOLDER),
                x -> x.node("filenames").isArray().isEmpty()
            );
        verify(folderService, times(1)).getStoredFiles(TEST_FOLDER);
    }

    @Test
    void testWhenRequestedFolderHasStoredFiles() throws Exception {
        final String path = "/folders/" + TEST_FOLDER;
        doReturn(Set.of(TestUtil.FILE_1, TestUtil.FILE_2)).when(folderService).getStoredFiles(TEST_FOLDER);

        final MvcResult mvcResult = mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andReturn();

        final String content = mvcResult.getResponse().getContentAsString();

        assertThatJson(content)
            .when(Option.IGNORING_ARRAY_ORDER)
            .and(
                x -> x.node("folder-name").isEqualTo(TEST_FOLDER),
                x -> x.node("filenames").isArray().isEqualTo(json("[\"file-1.mp4\",\"file-2.mp4\"]"))
            );
        verify(folderService, times(1)).getStoredFiles(TEST_FOLDER);
    }

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
                            .header("Authorization", AUTHORIZATION_TOKEN)
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
        String filename = "FT-0111-functionalTestFile5Mb_2020-05-19-16.45.11.123-UTC_0.mp4";
        Map<String, String> downloadInfo = Map.of(
            "filename", filename,
            "contentLength", "123123",
            "contentType", "video/mp4"
        );
        doReturn(downloadInfo).when(downloadService).getDownloadInfo(recordingId, 0);
        doNothing().when(downloadService).download(eq(filename), any(OutputStream.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d", recordingId, 0)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testShouldThrowSegmentDownloadException() throws Exception {
        UUID recordingId = UUID.randomUUID();
        String filename = "FT-0111-functionalTestFile5Mb_2020-05-19-16.45.11.123-UTC_0.mp4";
        Map<String, String> downloadInfo = Map.of(
            "filename", filename,
            "contentLength", "123123",
            "contentType", "video/mp4"
        );
        doReturn(downloadInfo).when(downloadService).getDownloadInfo(recordingId, 0);
        doThrow(new SegmentDownloadException("failed download"))
            .when(downloadService).download(eq(filename), any(OutputStream.class));

        mockMvc.perform(get(String.format("/hearing-recordings/%s/segments/%d", recordingId, 0)))
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    private void clogJobQueue() {
        IntStream.rangeClosed(1, INGESTION_QUEUE_SIZE + 10)
            .forEach(x -> {
                final HearingRecordingDto dto = HearingRecordingDto.builder()
                    .caseRef("cr" + x)
                    .build();
                ingestionQueue.offer(dto);
            });
    }
}
