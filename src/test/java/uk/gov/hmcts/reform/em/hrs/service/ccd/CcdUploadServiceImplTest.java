package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.storage.BlobIndexMarker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.RECORDING_REFERENCE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SEGMENT_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.TEST_FOLDER_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_FOLDER;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_FOLDER_NAME;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_HEARING_RECORDING_DTO;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_HEARING_RECORDING_WITH_SEGMENTS_1_3;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.VH_SEGMENT_1;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.hearingRecordingWithNoDataBuilder;

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
    @Mock
    private BlobIndexMarker blobIndexMarker;
    @Mock
    private CaseDataContentCreator caseDataCreator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private TtlService ttlService;

    @InjectMocks
    private CcdUploadServiceImpl underTest;

    private static final Optional<LocalDate> optTtl = Optional.empty();

    @Test
    void testShouldCreateNewCaseInCcdAndPersistRecordingAndSegmentToPostgresWhenHearingRecordingIsNotInDatabase() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(TEST_FOLDER_1.getName());

        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, optTtl);
        doReturn(recording).when(recordingRepository).saveAndFlush(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        doReturn(false).when(ttlService).isTtlEnabled();

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, optTtl);

        ArgumentCaptor<HearingRecording> hearingRecordingCaptor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(recordingRepository, times(2)).saveAndFlush(hearingRecordingCaptor.capture());
        List<HearingRecording> hearingRecordingList = hearingRecordingCaptor.getAllValues();
        var firstSave = hearingRecordingList.get(0);
        assertThat(firstSave.isTtlSet()).isFalse();
        assertThat(firstSave.getTtl()).isNull();
        assertThat(firstSave.getCcdCaseId()).isNull();
        var secondSave = hearingRecordingList.get(1);
        assertThat(secondSave.isTtlSet()).isFalse();
        assertThat(secondSave.getTtl()).isNull();
        assertThat(secondSave.getCcdCaseId()).isEqualTo(CCD_CASE_ID);

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, never()).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }

    @Test
    void testShouldCreateNewCaseWithTtlDetails() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(TEST_FOLDER_1.getName());
        LocalDate ttl = LocalDate.now();
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient)
            .createCase(recording.getId(), HEARING_RECORDING_DTO, Optional.of(ttl));
        doReturn(recording).when(recordingRepository).saveAndFlush(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        doReturn(true).when(ttlService).isTtlEnabled();
        doReturn(ttl).when(ttlService)
            .createTtl(HEARING_RECORDING_DTO.getServiceCode(),HEARING_RECORDING_DTO.getJurisdictionCode());

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, Optional.of(ttl));

        ArgumentCaptor<HearingRecording> hearingRecordingCaptor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(recordingRepository, times(2)).saveAndFlush(hearingRecordingCaptor.capture());
        List<HearingRecording> hearingRecordingList = hearingRecordingCaptor.getAllValues();
        var firstSave = hearingRecordingList.get(0);
        assertThat(firstSave.isTtlSet()).isFalse();
        assertThat(firstSave.getTtl()).isNull();
        assertThat(firstSave.getCcdCaseId()).isNull();
        var secondSave = hearingRecordingList.get(1);
        assertThat(secondSave.isTtlSet()).isTrue();
        assertThat(secondSave.getTtl()).isEqualTo(ttl);
        assertThat(secondSave.getCcdCaseId()).isEqualTo(CCD_CASE_ID);

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, never()).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }


    @Test
    void testShouldUpdateCaseInCcdAndPersistSegmentToPostgresWhenHearingRecordingReferenceExistsInDatabase() {
        doReturn(Optional.of(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());

        doReturn(CCD_CASE_ID)
            .when(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(HEARING_RECORDING_DTO)
            );
        doReturn(SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(HEARING_RECORDING_DTO)
            );
        verify(ccdDataStoreApiClient, never())
            .createCase(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId(), HEARING_RECORDING_DTO, optTtl);
        verify(recordingRepository, never()).saveAndFlush(any(HearingRecording.class));
        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, never()).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }

    @Test
    void testVhShouldCreateNewCaseInCcdAndPersistRecordingAndSegmentToPostgresWhenHearingRecordingIsNotInDatabase() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);
        doReturn(VH_FOLDER).when(folderService).getFolderByName(VH_FOLDER_NAME);

        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient)
            .createCase(recording.getId(), VH_HEARING_RECORDING_DTO, optTtl);
        doReturn(recording).when(recordingRepository).saveAndFlush(any(HearingRecording.class));
        doReturn(VH_SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        doReturn(false).when(ttlService).isTtlEnabled();

        underTest.upload(VH_HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);
        verify(ccdDataStoreApiClient).createCase(recording.getId(), VH_HEARING_RECORDING_DTO, optTtl);

        ArgumentCaptor<HearingRecording> hearingRecordingCaptor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(recordingRepository, times(2)).saveAndFlush(hearingRecordingCaptor.capture());
        List<HearingRecording> hearingRecordingList = hearingRecordingCaptor.getAllValues();
        assertThat(hearingRecordingList.get(0).isTtlSet()).isFalse();
        assertThat(hearingRecordingList.get(1).isTtlSet()).isFalse();

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, times(1)).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }

    @Test
    void testVhShouldUpdateCaseInCcdAndPersistSegmentToPostgresWhenHearingRecordingReferenceExistsInDatabase() {
        doReturn(Optional.of(VH_HEARING_RECORDING_WITH_SEGMENTS_1_3)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);

        doReturn(CCD_CASE_ID)
            .when(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(VH_HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(VH_HEARING_RECORDING_DTO)
            );
        doReturn(VH_SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));

        underTest.upload(VH_HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);
        verify(ccdDataStoreApiClient)
            .updateCaseData(
                anyLong(),
                eq(VH_HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId()),
                eq(VH_HEARING_RECORDING_DTO)
            );
        verify(ccdDataStoreApiClient, never())
            .createCase(VH_HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId(), VH_HEARING_RECORDING_DTO, optTtl);
        verify(recordingRepository, never()).saveAndFlush(any(HearingRecording.class));
        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, times(1)).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }

    @Test
    void testVhShouldNotUpdateCaseInCcdWhenHearingRecordingExistWithCcdIdInDatabase() {
        doReturn(Optional.of(VH_HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3)).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);

        underTest.upload(VH_HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, VH_FOLDER_NAME);
        verify(ccdDataStoreApiClient, never())
            .updateCaseData(
                anyLong(),
                any(UUID.class),
                any(HearingRecordingDto.class)
            );


        verify(ccdDataStoreApiClient, never())
            .createCase(any(UUID.class), any(HearingRecordingDto.class), ArgumentMatchers.any());
        verify(recordingRepository, never()).saveAndFlush(any(HearingRecording.class));
        verify(segmentRepository, never()).saveAndFlush(any(HearingRecordingSegment.class));
        verify(blobIndexMarker, times(1)).setProcessed(VH_HEARING_RECORDING_DTO.getFilename());
    }

    @Test
    void testShouldNotUpdateCaseWhenCcdIdIsNull() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();

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
            .createCase(any(UUID.class), any(HearingRecordingDto.class), ArgumentMatchers.any());
        verify(recordingRepository, never()).saveAndFlush(any(HearingRecording.class));
        verify(segmentRepository, never()).saveAndFlush(any(HearingRecordingSegment.class));
    }


}
