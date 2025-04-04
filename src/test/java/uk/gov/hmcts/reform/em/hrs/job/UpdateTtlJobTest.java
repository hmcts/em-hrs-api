package uk.gov.hmcts.reform.em.hrs.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private TtlService ttlService;
    @InjectMocks
    private UpdateTtlJob updateTtlJob;
    private HearingRecordingTtlMigrationDTO recordingDto;
    private List<HearingRecordingTtlMigrationDTO> recordingDtos;

    @BeforeEach
    void setUp() {
        openMocks(this);
        ReflectionTestUtils.setField(updateTtlJob, "threadLimit", 1);
        ReflectionTestUtils.setField(updateTtlJob, "batchSize", 1);
        ReflectionTestUtils.setField(updateTtlJob, "noOfIterations", 1);
        recordingDto = new HearingRecordingTtlMigrationDTO(
                UUID.randomUUID(), LocalDateTime.now().minusDays(1),
                "serviceCode","jurisdictionCode",123L);
        recordingDtos = List.of(recordingDto);
    }

    @Test
    void shouldRunUpdateTtlJobSuccessfully() {

        doReturn(recordingDtos).when(hearingRecordingRepository)
            .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));

        doReturn(LocalDate.now().plusDays(30)).when(ttlService)
            .createTtl(anyString(), anyString(), any(LocalDate.class));

        doNothing().when(ccdDataStoreApiClient)
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));

        doReturn(recordingDto).when(hearingRecordingRepository)
            .save(any(HearingRecording.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));
        verify(ttlService, times(1))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(1))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));
    }

    @Test
    void shouldNotRunUpdateTtlJobWhenNoRecordingsFound() {
        doReturn(List.of()).when(hearingRecordingRepository)
                .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));

        verify(ttlService, times(0))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(0))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));
    }

    @Test
    void shouldHandleExceptionDuringUpdateCaseWithTtl() {

        doReturn(recordingDtos).when(hearingRecordingRepository)
            .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));

        doReturn(LocalDate.now().plusDays(30)).when(ttlService)
            .createTtl(anyString(), anyString(), any(LocalDate.class));

        doThrow(new RuntimeException("CCD update failed")).when(ccdDataStoreApiClient)
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));

        updateTtlJob.run();

        verify(hearingRecordingRepository, times(1))
            .findByTtlSetFalseOrderByCreatedOnAsc(any(PageRequest.class));
        verify(ttlService, times(1))
            .createTtl(anyString(), anyString(), any(LocalDate.class));
        verify(ccdDataStoreApiClient, times(1))
            .updateCaseWithTtl(anyLong(), any(LocalDate.class));
    }

}
