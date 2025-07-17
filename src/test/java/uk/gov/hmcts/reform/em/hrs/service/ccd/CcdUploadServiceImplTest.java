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
    private CaseDataContentCreator caseDataCreator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private TtlService ttlService;

    @InjectMocks
    private CcdUploadServiceImpl underTest;

    private static final LocalDate ttl = LocalDate.now();

    @Test
    void testShouldCreateNewCaseInCcdAndPersistRecordingAndSegmentToPostgresWhenHearingRecordingIsNotInDatabase() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(TEST_FOLDER_1.getName());

        doReturn(ttl).when(ttlService)
            .createTtl(HEARING_RECORDING_DTO.getServiceCode(),
                       HEARING_RECORDING_DTO.getJurisdictionCode(),
                       LocalDate.now());
        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, ttl);
        doReturn(recording).when(recordingRepository).saveAndFlush(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));


        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, ttl);

        ArgumentCaptor<HearingRecording> hearingRecordingCaptor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(recordingRepository, times(2)).saveAndFlush(hearingRecordingCaptor.capture());
        List<HearingRecording> hearingRecordingList = hearingRecordingCaptor.getAllValues();
        var firstSave = hearingRecordingList.get(0);
        assertThat(firstSave.getTtl()).isNull();
        assertThat(firstSave.getCcdCaseId()).isNull();
        var secondSave = hearingRecordingList.get(1);
        assertThat(secondSave.isTtlSet()).isTrue();
        assertThat(secondSave.getTtl()).isNotNull();
        assertThat(secondSave.getCcdCaseId()).isEqualTo(CCD_CASE_ID);

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
    }

    @Test
    void testShouldCreateNewCaseWithTtlDetails() {
        HearingRecording recording = hearingRecordingWithNoDataBuilder();
        doReturn(Optional.empty()).when(recordingRepository)
            .findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        doReturn(TEST_FOLDER_1).when(folderService).getFolderByName(TEST_FOLDER_1.getName());

        doReturn(CCD_CASE_ID).when(ccdDataStoreApiClient)
            .createCase(recording.getId(), HEARING_RECORDING_DTO, ttl);
        doReturn(recording).when(recordingRepository).saveAndFlush(any(HearingRecording.class));
        doReturn(SEGMENT_1).when(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
        doReturn(ttl).when(ttlService)
            .createTtl(HEARING_RECORDING_DTO.getServiceCode(),
                       HEARING_RECORDING_DTO.getJurisdictionCode(),
                       LocalDate.now());

        underTest.upload(HEARING_RECORDING_DTO);

        verify(recordingRepository).findByRecordingRefAndFolderName(RECORDING_REFERENCE, TEST_FOLDER_1.getName());
        verify(ccdDataStoreApiClient).createCase(recording.getId(), HEARING_RECORDING_DTO, ttl);

        ArgumentCaptor<HearingRecording> hearingRecordingCaptor = ArgumentCaptor.forClass(HearingRecording.class);
        verify(recordingRepository, times(2)).saveAndFlush(hearingRecordingCaptor.capture());
        List<HearingRecording> hearingRecordingList = hearingRecordingCaptor.getAllValues();
        var firstSave = hearingRecordingList.get(0);
        assertThat(firstSave.getTtl()).isNull();
        assertThat(firstSave.getCcdCaseId()).isNull();
        var secondSave = hearingRecordingList.get(1);
        assertThat(secondSave.getTtl()).isEqualTo(ttl);
        assertThat(secondSave.getCcdCaseId()).isEqualTo(CCD_CASE_ID);

        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
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
            .createCase(HEARING_RECORDING_WITH_SEGMENTS_1_2_and_3.getId(), HEARING_RECORDING_DTO, ttl);
        verify(recordingRepository, never()).saveAndFlush(any(HearingRecording.class));
        verify(segmentRepository).saveAndFlush(any(HearingRecordingSegment.class));
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
