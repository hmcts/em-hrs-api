package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDateTime;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    private static final Long CCD_CASE_ID = 1234567890L;

    private HearingRecordingDto mockDto;

    @BeforeEach
    void setUp() {
        mockDto = mock(HearingRecordingDto.class);
        lenient().when(mockDto.getRecordingDateTime()).thenReturn(LocalDateTime.now());
    }

    @Test
    void testShouldCreateNewCaseWhenHearingRecordingIsNotInDatabase() {
        HearingRecording newRecording = new HearingRecording();
        HearingRecording recordingWithCcdId = new HearingRecording();
        recordingWithCcdId.setCcdCaseId(CCD_CASE_ID);

        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(mockDto);
        doReturn(newRecording).when(hearingRecordingService).createHearingRecording(mockDto);
        doReturn(A_TTL_DATE).when(ttlService).createTtl(any(), any(), any());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(any(), eq(mockDto), eq(A_TTL_DATE));
        doReturn(recordingWithCcdId).when(hearingRecordingService).updateCcdCaseId(newRecording, CCD_CASE_ID);

        underTest.upload(mockDto);

        verify(hearingRecordingService).findHearingRecording(mockDto);
        verify(hearingRecordingService).createHearingRecording(mockDto);
        verify(ccdDataStoreApiClient).createCase(newRecording.getId(), mockDto, A_TTL_DATE);
        verify(hearingRecordingService).updateCcdCaseId(newRecording, CCD_CASE_ID);
        verify(segmentService).createAndSaveSegment(any(HearingRecording.class), eq(mockDto));
        verify(ccdDataStoreApiClient, never()).updateCaseData(anyLong(), any(), any());
    }

    @Test
    void testShouldUpdateCaseWhenHearingRecordingExistsInDatabase() {
        HearingRecording existingRecording = new HearingRecording();
        existingRecording.setCcdCaseId(CCD_CASE_ID);

        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(mockDto);
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).updateCaseData(
            anyLong(), any(), any()
        );

        underTest.upload(mockDto);

        verify(ccdDataStoreApiClient).updateCaseData(anyLong(), any(), any());
        verify(segmentService).createAndSaveSegment(existingRecording, mockDto);
        verify(hearingRecordingService, never()).createHearingRecording(any());
    }

    @Test
    void testUpdateCaseShouldHandleConstraintViolationExceptionWhenSegmentExists() {
        HearingRecording existingRecording = new HearingRecording();
        existingRecording.setCcdCaseId(CCD_CASE_ID);

        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(mockDto);
        doThrow(new ConstraintViolationException("test violation", null, null))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertDoesNotThrow(() -> underTest.upload(mockDto));

        verify(segmentService).createAndSaveSegment(existingRecording, mockDto);
    }

    @Test
    void testUpdateCaseShouldPropagateOtherExceptionsWhenSegmentCreationFails() {
        HearingRecording existingRecording = new HearingRecording();
        existingRecording.setCcdCaseId(CCD_CASE_ID);

        doReturn(Optional.of(existingRecording))
            .when(hearingRecordingService).findHearingRecording(mockDto);
        doThrow(new RuntimeException("Generic DB error"))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertThrows(RuntimeException.class, () -> underTest.upload(mockDto));

        verify(segmentService).createAndSaveSegment(existingRecording, mockDto);
    }

    @Test
    void testCreateCaseShouldPropagateExceptionWhenSegmentCreationFails() {
        HearingRecording newRecording = new HearingRecording();

        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(mockDto);
        doReturn(newRecording).when(hearingRecordingService).createHearingRecording(mockDto);
        doReturn(A_TTL_DATE).when(ttlService).createTtl(any(), any(), any());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(any(), eq(mockDto), eq(A_TTL_DATE));
        doReturn(newRecording).when(hearingRecordingService).updateCcdCaseId(newRecording, CCD_CASE_ID);

        doThrow(new RuntimeException("Generic DB error"))
            .when(segmentService).createAndSaveSegment(any(), any());

        assertThrows(RuntimeException.class, () -> underTest.upload(mockDto));
    }

    @Test
    void testShouldNotCallCcdOrSegmentApiWhenRecordingExistsButCcdIdIsNull() {
        HearingRecording recordingWithNullCcdId = new HearingRecording();
        recordingWithNullCcdId.setCcdCaseId(null);

        doReturn(Optional.of(recordingWithNullCcdId)).when(hearingRecordingService)
            .findHearingRecording(mockDto);

        underTest.upload(mockDto);

        verify(hearingRecordingService).findHearingRecording(mockDto);
        verify(ccdDataStoreApiClient, never())
            .updateCaseData(anyLong(), any(UUID.class), any(HearingRecordingDto.class));
        verify(ccdDataStoreApiClient, never()).createCase(any(UUID.class), any(HearingRecordingDto.class), any());
        verify(segmentService, never()).createAndSaveSegment(any(), any());
    }

    @Test
    void testShouldRethrowCcdUploadExceptionWhenHearingRecordingCreationFails() {
        doReturn(Optional.empty()).when(hearingRecordingService).findHearingRecording(mockDto);
        doThrow(new CcdUploadException("Hearing Recording already exists."))
            .when(hearingRecordingService).createHearingRecording(mockDto);

        final CcdUploadException e = assertThrows(
            CcdUploadException.class,
            () -> underTest.upload(mockDto)
        );

        assertEquals("Hearing Recording already exists.", e.getMessage());
        verify(ccdDataStoreApiClient, never()).createCase(any(), any(), any());
        verify(segmentService, never()).createAndSaveSegment(any(), any());
    }
}
