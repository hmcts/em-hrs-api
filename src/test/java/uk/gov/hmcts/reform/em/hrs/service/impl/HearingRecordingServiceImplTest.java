package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HearingRecordingServiceImplTest {

    private final List<HearingRecording> recordings = new ArrayList<>();
    private final List<HearingRecordingSegment> segments = new ArrayList<>();

    @Mock
    private HearingRecordingRepository recordingRepository;
    @Mock
    private BlobStorageDeleteService deleteService;
    @InjectMocks
    private HearingRecordingServiceImpl recordingService;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 2; ++i) {
            HearingRecording recording = new HearingRecording();
            recordings.add(recording);
            recording.setHearingSource(HearingSource.CVP.toString());
            recording.setSegments(new HashSet<>());
            for (int j = 0; j < 3; ++j) {
                HearingRecordingSegment segment = new HearingRecordingSegment();
                segment.setFilename(RandomStringUtils.randomAlphanumeric(9));
                segment.setHearingRecording(recording);
                segments.add(segment);
                recording.getSegments().add(segment);
            }
        }
    }

    @Test
    void deleteCaseHearingRecordings() {
        List<Long> caseIds = Collections.emptyList();
        doReturn(recordings).when(recordingRepository).deleteByCcdCaseIdIn(caseIds);
        recordingService.deleteCaseHearingRecordings(caseIds);
        for (HearingRecordingSegment segment : segments) {
            HearingSource source = HearingSource.valueOf(segment.getHearingRecording().getHearingSource());
            verify(deleteService).deleteBlob(segment.getFilename(), source);
        }

    }

    @Test
    void databaseFailureDoesNotDeleteBlobs() {
        List<Long> caseIds = List.of(12L,23L);
        doThrow(RuntimeException.class).when(recordingRepository).deleteByCcdCaseIdIn(caseIds);
        recordingService.deleteCaseHearingRecordings(caseIds);
        verify(deleteService, never()).deleteBlob(anyString(), any());
    }

}
