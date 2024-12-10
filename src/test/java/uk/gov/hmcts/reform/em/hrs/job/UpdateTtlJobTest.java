package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

class UpdateTtlJobTest {

    @Mock
    private HearingRecordingRepository hearingRecordingRepository;
    @Mock
    private CcdDataStoreApiClient ccdDataStoreApiClient;
    @Mock
    private TtlService ttlService;
    @InjectMocks
    private UpdateTtlJob updateTtlJob;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void shouldRunUpdateTtlJobSuccessfully() {
        HearingRecording recording = new HearingRecording();
        recording.setCcdCaseId(123L);
        recording.setServiceCode("serviceCode");
        recording.setJurisdictionCode("jurisdictionCode");
        recording.setCreatedOn(LocalDateTime.now().minusDays(1));
        List<HearingRecording> recordings = List.of(recording);

        doReturn(recordings).when(hearingRecordingRepository)
            .findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));

        doReturn(LocalDate.now().plusDays(30)).when(ttlService)
            .createTtl(anyString(), anyString(), any(LocalDate.class));

        doNothing().when(ccdDataStoreApiClient)
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));

        doReturn(recording).when(hearingRecordingRepository)
            .save(any(HearingRecording.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));
        verify(ttlService, times(1))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(1))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));
        verify(hearingRecordingRepository, times(1))
            .save(any(HearingRecording.class));
    }

    @Test
    void shouldNotRunUpdateTtlJobWhenNoRecordingsFound() {
        doReturn(List.of()).when(hearingRecordingRepository).findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));

        verify(ttlService, times(0))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(0))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));
        verify(hearingRecordingRepository, times(0))
            .save(any(HearingRecording.class));
    }

    @Test
    void shouldHandleExceptionDuringUpdateCaseWithTtl() {
        HearingRecording recording = new HearingRecording();
        recording.setCcdCaseId(123L);
        recording.setServiceCode("serviceCode");
        recording.setJurisdictionCode("jurisdictionCode");
        recording.setCreatedOn(LocalDateTime.now().minusDays(1));
        List<HearingRecording> recordings = List.of(recording);

        doReturn(recordings).when(hearingRecordingRepository)
            .findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));

        doReturn(LocalDate.now().plusDays(30)).when(ttlService)
            .createTtl(anyString(), anyString(), any(LocalDate.class));

        doThrow(new RuntimeException("CCD update failed")).when(ccdDataStoreApiClient)
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(Limit.class));
        verify(ttlService, times(1))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(1))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));

        verify(hearingRecordingRepository, times(0))
            .save(any(HearingRecording.class));
    }

}
