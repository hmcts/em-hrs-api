package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.TtlService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CcdUploadServiceImpl implements CcdUploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcdUploadServiceImpl.class);

    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final HearingRecordingRepository recordingRepository;
    private final HearingRecordingSegmentRepository segmentRepository;
    private final FolderService folderService;
    private final TtlService ttlService;

    @Autowired
    public CcdUploadServiceImpl(
        final CcdDataStoreApiClient ccdDataStoreApiClient,
        final HearingRecordingRepository recordingRepository,
        final HearingRecordingSegmentRepository segmentRepository,
        final FolderService folderService,
        final TtlService ttlService
    ) {
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.recordingRepository = recordingRepository;
        this.segmentRepository = segmentRepository;
        this.folderService = folderService;
        this.ttlService = ttlService;
    }

    @Override
    public void upload(final HearingRecordingDto recordingDto) {

        String recordingRef = recordingDto.getRecordingRef();
        String folder = recordingDto.getFolder();

        LOGGER.info("determining if recording (ref {}) in folder {}) has entry in CCD", recordingRef, folder);

        final Optional<HearingRecording> hearingRecording =
            recordingRepository.findByRecordingRefAndFolderName(recordingRef, folder);

        Long caseId;
        if (hearingRecording.isPresent()) {
            caseId = updateCase(hearingRecording.get(), recordingDto);
        } else {
            caseId = createCaseinCcdAndPersist(recordingDto);
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
            HearingRecordingSegment segment = createSegment(recording, recordingDto);
            segmentRepository.saveAndFlush(segment);
        } catch (ConstraintViolationException e) {
            LOGGER.warn(
                "Segment not added to database, which is acceptable for duplicate segments (ref {}), (ccdId {})",
                recordingRef,
                ccdCaseId
            );
        }

        return caseDetailsId;
    }

    private Long createCaseinCcdAndPersist(final HearingRecordingDto recordingDto) {
        LOGGER.info("creating a new case for recording: {}", recordingDto.getRecordingRef());

        var folder = folderService.getFolderByName(recordingDto.getFolder());

        HearingRecording recording = HearingRecording.builder()
            .folder(folder)
            .recordingRef(recordingDto.getRecordingRef())
            .caseRef(recordingDto.getCaseRef())
            .hearingLocationCode(recordingDto.getCourtLocationCode())
            .hearingRoomRef(recordingDto.getHearingRoomRef())
            .hearingSource(recordingDto.getRecordingSource().name())
            .jurisdictionCode(recordingDto.getJurisdictionCode())
            .serviceCode(recordingDto.getServiceCode())
            .createdOn(LocalDateTime.now())
            .build();

        try {
            recording = recordingRepository.saveAndFlush(recording);

        } catch (ConstraintViolationException e) {
            //the recording has already been persisted by another cluster - do not proceed as waiting for CCD id
            LOGGER.warn("Hearing Recording already exists in database.");
            throw new CcdUploadException("Hearing Recording already exists. Likely race condition from another server");
        } catch (Exception e) {
            LOGGER.warn(
                "create case Unhandled Exception whilst adding segment to DB (ref {}) to case(ccdId {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
            throw new CcdUploadException("Unhandled Exception trying to persist case");
        }

        LOGGER.info("About to create case in CCD");

        recording.setTtlSet(true);
        var ttl = ttlService.createTtl(
            recordingDto.getServiceCode(),
            recordingDto.getJurisdictionCode(),
            recordingDto.getRecordingDateTime().toLocalDate()
        );
        recording.setTtl(ttl);

        final Long caseId = ccdDataStoreApiClient.createCase(recording.getId(), recordingDto, ttl);
        recording.setCcdCaseId(caseId);
        recording = recordingRepository.saveAndFlush(recording);
        LOGGER.info("Created case in CCD: {} for  {} ", caseId, recordingDto.getRecordingSource());

        HearingRecordingSegment segment = createSegment(recording, recordingDto);
        segmentRepository.saveAndFlush(segment);
        return caseId;
    }

    private HearingRecordingSegment createSegment(
        final HearingRecording recording,
        final HearingRecordingDto recordingDto
    ) {
        return HearingRecordingSegment.builder()
            .filename(recordingDto.getFilename())
            .fileExtension(recordingDto.getFilenameExtension())
            .fileSizeMb(recordingDto.getFileSize())
            .fileMd5Checksum(recordingDto.getCheckSum())
            .ingestionFileSourceUri(recordingDto.getSourceBlobUrl())
            .recordingSegment(recordingDto.getSegment())
            .hearingRecording(recording)
            .interpreter(recordingDto.getInterpreter())
            .build();
    }
}
