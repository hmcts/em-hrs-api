package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;
import uk.gov.hmcts.reform.em.hrs.service.SegmentService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CcdUploadServiceImpl implements CcdUploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcdUploadServiceImpl.class);

    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final HearingRecordingService hearingRecordingService;
    private final SegmentService segmentService;
    private final TtlService ttlService;

    @Autowired
    public CcdUploadServiceImpl(
        final CcdDataStoreApiClient ccdDataStoreApiClient,
        final HearingRecordingService hearingRecordingService,
        final SegmentService segmentService,
        final TtlService ttlService
    ) {
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.hearingRecordingService = hearingRecordingService;
        this.segmentService = segmentService;
        this.ttlService = ttlService;
    }

    @Override
    public void upload(final HearingRecordingDto recordingDto) {
        String recordingRef = recordingDto.getRecordingRef();
        String folder = recordingDto.getFolder();

        LOGGER.info("determining if recording (ref {}) in folder {}) has entry in CCD", recordingRef, folder);

        final Optional<HearingRecording> hearingRecordingOptional =
            hearingRecordingService.findHearingRecording(recordingDto);

        Long caseId;
        if (hearingRecordingOptional.isPresent()) {
            caseId = updateCase(hearingRecordingOptional.get(), recordingDto);
        } else {
            HearingRecording newHearingRecording = hearingRecordingService.createHearingRecording(recordingDto);
            caseId = createCaseInCcd(newHearingRecording, recordingDto);
        }

        // this is for dynatrace, do not change
        LOGGER.info(
            "Hearing recording processed successfully, ref:{}, source: {}, ccd caseId:{}",
            recordingRef,
            recordingDto.getRecordingSource(),
            caseId
        );
    }

    private Long updateCase(final HearingRecording recording, final HearingRecordingDto recordingDto) {
        Long ccdCaseId = recording.getCcdCaseId();
        String recordingRef = recordingDto.getRecordingRef();
        String folder = recordingDto.getFolder();

        if (Objects.isNull(ccdCaseId)) {
            LOGGER.info(
                "Recording Ref {} in folder {}, has no ccd id, case still being created in CCD or has been rejected",
                recordingRef, folder
            );
            return null;
        }

        LOGGER.info(
            "adding  recording (ref {}) in folder {} to case (ccdId {})", recordingRef, folder, ccdCaseId);

        UUID id = recording.getId();
        Long caseDetailsId =
            ccdDataStoreApiClient.updateCaseData(ccdCaseId, id, recordingDto);

        LOGGER.info("Case Details (id {}) updated successfully", caseDetailsId);

        try {
            segmentService.createAndSaveSegment(recording, recordingDto);
        } catch (ConstraintViolationException e) {
            LOGGER.warn(
                "Segment not added to database, which is acceptable for duplicate segments (ref {}), (ccdId {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        }

        return caseDetailsId;
    }

    private Long createCaseInCcd(final HearingRecording recording, final HearingRecordingDto recordingDto) {
        LOGGER.info("About to create case in CCD");

        var ttl = ttlService.createTtl(
            recordingDto.getServiceCode(),
            recordingDto.getJurisdictionCode(),
            recordingDto.getRecordingDateTime().toLocalDate()
        );
        recording.setTtl(ttl);

        final Long caseId = ccdDataStoreApiClient.createCase(recording.getId(), recordingDto, ttl);
        hearingRecordingService.updateCcdCaseId(recording, caseId);

        LOGGER.info("Created case in CCD: {} for  {} ", caseId, recordingDto.getRecordingSource());

        segmentService.createAndSaveSegment(recording, recordingDto);

        return caseId;
    }
}
