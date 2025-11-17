package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.hearingRecordingWithNoDataBuilder;

@ExtendWith(MockitoExtension.class)
class CcdUploadServiceImplTest {

    @Mock
    private HearingRecordingService hearingRecordingService;
    @Mock
    private SegmentService segmentService;
    @Mock
    private CcdDataStoreApiClient ccdDataStoreApiClient;
    @Mock
    private TtlService ttlService;

    @InjectMocks
    private CcdUploadServiceImpl underTest;

    private static final LocalDate A_TTL_DATE = LocalDate.now();

    @Test
    void testShouldCreateNewCaseWhenHearingRecordingIsNotInDatabase() {
        HearingRecording newRecording = hearingRecordingWithNoDataBuilder();
        HearingRecording recordingWithCcdId = hearingRecordingWithNoDataBuilder();
        recordingWithCcdId.setCcdCaseId(CCD_CASE_ID);

        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doReturn(newRecording).when(hearingRecordingService).createHearingRecording(HEARING_RECORDING_DTO);
        doReturn(A_TTL_DATE).when(ttlService).createTtl(any(), any(), any());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(any(), eq(HEARING_RECORDING_DTO), eq(A_TTL_DATE));
        doReturn(recordingWithCcdId).when(hearingRecordingService).updateCcdCaseId(newRecording, CCD_CASE_ID);

        underTest.upload(HEARING_RECORDING_DTO);

        verify(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        verify(hearingRecordingService).createHearingRecording(HEARING_RECORDING_DTO);
        verify(ccdDataStoreApiClient).createCase(newRecording.getId(), HEARING_RECORDING_DTO, A_TTL_DATE);
        verify(hearingRecordingService).updateCcdCaseId(newRecording, CCD_CASE_ID);
        verify(segmentService).createAndSaveSegment(any(HearingRecording.class), eq(HEARING_RECORDING_DTO));
        verify(ccdDataStoreApiClient, never()).updateCaseData(anyLong(), any(), any());
    }

    @Test
    void testShouldUpdateCaseWhenHearingRecordingExistsInDatabase() {
        HearingRecording existingRecording = HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;

        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).updateCaseData(
            anyLong(), any(), any()
        );

        underTest.upload(HEARING_RECORDING_DTO);

        verify(ccdDataStoreApiClient).updateCaseData(anyLong(), any(), any());
        verify(segmentService).createAndSaveSegment(existingRecording, HEARING_RECORDING_DTO);
        verify(hearingRecordingService, never()).createHearingRecording(any());
    }

    @Test
    void testUpdateCaseShouldHandleConstraintViolationExceptionWhenSegmentExists() {
        HearingRecording existingRecording = HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doThrow(new ConstraintViolationException("test violation", null, null))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertDoesNotThrow(() -> underTest.upload(HEARING_RECORDING_DTO));

        verify(segmentService).createAndSaveSegment(existingRecording, HEARING_RECORDING_DTO);
    }

    @Test
    void testUpdateCaseShouldPropagateOtherExceptionsWhenSegmentCreationFails() {
        HearingRecording existingRecording = HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doThrow(new RuntimeException("Generic DB error"))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertThrows(RuntimeException.class, () -> underTest.upload(HEARING_RECORDING_DTO));

        verify(segmentService).createAndSaveSegment(existingRecording, HEARING_RECORDING_DTO);
    }

    @Test
    void testCreateCaseShouldPropagateExceptionWhenSegmentCreationFails() {
        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doReturn(hearingRecordingWithNoDataBuilder()).when(hearingRecordingService).createHearingRecording(any());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(any(), any(), any());

        doThrow(new RuntimeException("Generic DB error"))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertThrows(RuntimeException.class, () -> underTest.upload(HEARING_RECORDING_DTO));
    }


    @Test
    void testShouldNotCallCcdOrSegmentApiWhenRecordingExistsButCcdIdIsNull() {
        HearingRecording recordingWithNullCcdId = hearingRecordingWithNoDataBuilder();
        recordingWithNullCcdId.setCcdCaseId(null);

        doReturn(Optional.of(recordingWithNullCcdId)).when(hearingRecordingService)
            .findHearingRecording(HEARING_RECORDING_DTO);

        underTest.upload(HEARING_RECORDING_DTO);

        verify(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        verify(ccdDataStoreApiClient, never())
            .updateCaseData(anyLong(), any(UUID.class), any(HearingRecordingDto.class));
        verify(ccdDataStoreApiClient, never()).createCase(any(UUID.class), any(HearingRecordingDto.class), any());
        verify(segmentService, never()).createAndSaveSegment(any(), any());
    }

    @Test
    void testShouldRethrowCcdUploadExceptionWhenHearingRecordingCreationFails() {
        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(HEARING_RECORDING_DTO);
        doThrow(new CcdUploadException("Hearing Recording already exists."))
            .when(hearingRecordingService).createHearingRecording(HEARING_RECORDING_DTO);

        final CcdUploadException e = assertThrows(
            CcdUploadException.class,
            () -> underTest.upload(HEARING_RECORDING_DTO)
        );

        assertEquals("Hearing Recording already exists.", e.getMessage());
        verify(ccdDataStoreApiClient, never()).createCase(any(), any(), any());
        verify(segmentService, never()).createAndSaveSegment(any(), any());
    }
}
