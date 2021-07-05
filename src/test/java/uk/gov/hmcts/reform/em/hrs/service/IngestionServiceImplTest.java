package uk.gov.hmcts.reform.em.hrs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {
    @Mock
    private CcdDataStoreApiClient ccdDataStoreApiClient;
    @Mock
    private HearingRecordingRepository recordingRepository;
    @Mock
    private HearingRecordingSegmentRepository segmentRepository;
    @Mock
    private HearingRecordingStorage hearingRecordingStorage;
    @Mock
    private Snooper snooper;
    @Mock
    private FolderService folderService;

    @InjectMocks
    private IngestionServiceImpl underTest;

    @Test
    void testShouldCreateNewCaseInCcdAndPersistToPostgresAndAzureStorageWhenHearingRecordingIsNew() {
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER.getName());

        doReturn(TEST_FOLDER).when(folderService).getFolderByName(TEST_FOLDER.getName());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(HEARING_RECORDING.getId(), HEARING_RECORDING_DTO);
        doReturn(HEARING_RECORDING).when(recordingRepository).save(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).save(any(HearingRecordingSegment.class));
        doNothing().when(hearingRecordingStorage)
            .copyRecording(HEARING_RECORDING_DTO.getCvpFileUrl(), HEARING_RECORDING_DTO.getFilename());

        underTest.ingest(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER.getName());
        verify(ccdDataStoreApiClient).createCase(HEARING_RECORDING.getId(), HEARING_RECORDING_DTO);
        verify(recordingRepository, times(2)).save(any(HearingRecording.class));
        verify(segmentRepository).save(any(HearingRecordingSegment.class));
        verify(hearingRecordingStorage)
            .copyRecording(HEARING_RECORDING_DTO.getCvpFileUrl(), HEARING_RECORDING_DTO.getFilename());
        verifyNoInteractions(snooper);
    }

    @Test
    void testShouldUpdateCaseInCcdAndPersistToPostgresAndAzureStorageWhenHearingRecordingExist() {
        doReturn(Optional.of(HEARING_RECORDING_WITH_SEGMENTS)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER.getName());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient)
            .updateCaseData(anyLong(), eq(HEARING_RECORDING_WITH_SEGMENTS.getId()), eq(HEARING_RECORDING_DTO));
        doReturn(SEGMENT_1).when(segmentRepository).save(any(HearingRecordingSegment.class));
        doNothing().when(hearingRecordingStorage)
            .copyRecording(HEARING_RECORDING_DTO.getCvpFileUrl(), HEARING_RECORDING_DTO.getFilename());

        underTest.ingest(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER.getName());
        verify(ccdDataStoreApiClient)
            .updateCaseData(anyLong(), eq(HEARING_RECORDING_WITH_SEGMENTS.getId()), eq(HEARING_RECORDING_DTO));
        verify(ccdDataStoreApiClient, never())
            .createCase(HEARING_RECORDING_WITH_SEGMENTS.getId(), HEARING_RECORDING_DTO);
        verify(recordingRepository, never()).save(any(HearingRecording.class));
        verify(segmentRepository).save(any(HearingRecordingSegment.class));
        verify(hearingRecordingStorage)
            .copyRecording(HEARING_RECORDING_DTO.getCvpFileUrl(), HEARING_RECORDING_DTO.getFilename());
        verifyNoInteractions(snooper);
    }
}
