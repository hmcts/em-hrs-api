package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_NO_DATA_BUILDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_1;

@ExtendWith(MockitoExtension.class)
class CcdUploadServiceImplTest {
    @Mock
    private CcdDataStoreApiClient ccdDataStoreApiClient;
    @Mock
    private HearingRecordingRepository recordingRepository;
    @Mock
    private HearingRecordingSegmentRepository segmentRepository;
    @Mock
    private FolderService folderService;

    @InjectMocks
    private CcdUploadServiceImpl underTest;

    @Test
    void testShouldCreateNewCaseInCcdAndPersistRecordingAndSegmentToPostgresWhenHearingRecordingIsNotInDatabase() {
        HearingRecording recording = HEARING_RECORDING_WITH_NO_DATA_BUILDER();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(TEST_FOLDER_1.getName());

        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO);
        doReturn(recording).when(recordingRepository).save(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).save(any(HearingRecordingSegment.class));

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO);
        verify(recordingRepository, times(2)).save(any(HearingRecording.class));
        verify(segmentRepository).save(any(HearingRecordingSegment.class));
    }

    @Test
    void testShouldUpdateCaseInCcdAndPersistSegmentToPostgresWhenHearingRecordingReferenceExistsInDatabase() {
        doReturn(Optional.of(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(HEARING_RECORDING_DTO)
        );
        doReturn(SEGMENT_1).when(segmentRepository).save(any(HearingRecordingSegment.class));

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(HEARING_RECORDING_DTO)
        );
        verify(ccdDataStoreApiClient, never())
            .createCase(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId(), HEARING_RECORDING_DTO);
        verify(recordingRepository, never()).save(any(HearingRecording.class));
        verify(segmentRepository).save(any(HearingRecordingSegment.class));
    }

    @Test
    void testShouldNotUpdateCaseWhenCcdIdIsNull() {
        HearingRecording recording = HEARING_RECORDING_WITH_NO_DATA_BUILDER();

        doReturn(Optional.of(recording)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient, never())
            .updateCaseData(
                anyLong(),
                any(UUID.class),
                any(HearingRecordingDto.class)
            );
        verify(ccdDataStoreApiClient, never())
            .createCase(any(UUID.class), any(HearingRecordingDto.class));
        verify(recordingRepository, never()).save(any(HearingRecording.class));
        verify(segmentRepository, never()).save(any(HearingRecordingSegment.class));
    }


}
