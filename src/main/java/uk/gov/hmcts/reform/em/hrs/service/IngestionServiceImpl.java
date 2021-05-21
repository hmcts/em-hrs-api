package uk.gov.hmcts.reform.em.hrs.service;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdDataStoreApiClient;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.util.Optional;

@Service
@Transactional
public class IngestionServiceImpl implements IngestionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionServiceImpl.class);

    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final HearingRecordingRepository recordingRepository;
    private final HearingRecordingSegmentRepository segmentRepository;
    private final HearingRecordingStorage hearingRecordingStorage;
    private final Snooper snooper;
    private final FolderService folderService;

    @Autowired
    public IngestionServiceImpl(final CcdDataStoreApiClient ccdDataStoreApiClient,
                                final HearingRecordingRepository recordingRepository,
                                final HearingRecordingSegmentRepository segmentRepository,
                                final HearingRecordingStorage hearingRecordingStorage,
                                final Snooper snooper,
                                final FolderService folderService) {
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.recordingRepository = recordingRepository;
        this.segmentRepository = segmentRepository;
        this.hearingRecordingStorage = hearingRecordingStorage;
        this.snooper = snooper;
        this.folderService = folderService;
    }

    @Override
    @Async("HrsAsyncExecutor")
    public void ingest(final HearingRecordingDto hearingRecordingDto) {

        hearingRecordingStorage
            .copyRecording(hearingRecordingDto.getCvpFileUrl(), hearingRecordingDto.getFilename());

        final Optional<HearingRecording> checkHearingRecording =
            recordingRepository.findByRecordingRefAndFolderName(
                hearingRecordingDto.getRecordingRef(), hearingRecordingDto.getFolder()
            );

        checkHearingRecording.ifPresentOrElse(
            hearingRecording -> updateCase(hearingRecording, hearingRecordingDto),
            () -> createCaseinCcdAndPersist(hearingRecordingDto)
        );

    }

    private void updateCase(final HearingRecording recording,
                            final HearingRecordingDto recordingDto) {
        if (recording.getCcdCaseId() == null) {
            LOGGER.info(
                "Recording Ref {} in folder {}, has no ccd id, case still being created in CCD or has been rejected",
                recordingDto.getRecordingRef(),
                recordingDto.getFolder()
            );
            return;
        }

        LOGGER.info(
            "adding  recording (ref {}) in folder {} to case (ccdid {})",
            recordingDto.getRecordingRef(),
            recordingDto.getFolder(),
            recording.getCcdCaseId()
        );


        Long caseDetailsId =
            ccdDataStoreApiClient.updateCaseData(recording.getCcdCaseId(), recording.getId(), recordingDto);

        LOGGER.info("Case Details (id {}) updated successfully", caseDetailsId);

        try {
            HearingRecordingSegment segment = createSegment(recording, recordingDto);
            segmentRepository.save(segment);

        } catch (ConstraintViolationException e) {
            //TODO this is not caught as the sql is a multie step / tranasction so throws a batch exception which has
            //potentially multiple exceptions. Consider catching these or improving the messaging.
            LOGGER.info(
                "updateCase ConstraintViolationException segment already added to DB (ref {}) to case(ccdid {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        } catch (Exception e) {
            LOGGER.info(
                "segment not added to database, probably duplicate entry (ref {}) to case(ccdid {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        }

        LOGGER.info("updateCase end");
    }

    private void createCaseinCcdAndPersist(final HearingRecordingDto recordingDto) {
        LOGGER.info("creating a new case for recording: {}", recordingDto.getRecordingRef());

        final Folder folder = folderService.getFolderByName(recordingDto.getFolder());

        HearingRecording recording = HearingRecording.builder()
            .folder(folder)
            .recordingRef(recordingDto.getRecordingRef())
            .caseRef(recordingDto.getCaseRef())
            .hearingLocationCode(recordingDto.getCourtLocationCode())
            .hearingRoomRef(recordingDto.getHearingRoomRef())
            .hearingSource(recordingDto.getRecordingSource())
            .jurisdictionCode(recordingDto.getJurisdictionCode())
            .serviceCode(recordingDto.getServiceCode())
            .createdOn(recordingDto.getRecordingDateTime())
            .build();

        try {
            recording = recordingRepository.save(recording);

        } catch (ConstraintViolationException e) {
            //the recording has already been persisted by another cluster - do not proceed as waiting for CCD id
            LOGGER
                .info(
                    "create case Hearing Recording already exists in database, not persisting recording, nor segment "
                        + "at this time");
        } catch (Exception e) {
            LOGGER.info(
                "create case Unhandled Exception whilst adding segment to DB (ref {}) to case(ccdid {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        }


        final Long caseId = ccdDataStoreApiClient.createCase(recording.getId(), recordingDto);
        recording.setCcdCaseId(caseId);
        recording = recordingRepository.save(recording);

        HearingRecordingSegment segment = createSegment(recording, recordingDto);
        segmentRepository.save(segment);
    }

    private HearingRecordingSegment createSegment(final HearingRecording recording,
                                                  final HearingRecordingDto recordingDto) {
        return HearingRecordingSegment.builder()
            .filename(recordingDto.getFilename())
            .fileExtension(recordingDto.getFilenameExtension())
            .fileSizeMb(recordingDto.getFileSize())
            .fileMd5Checksum(recordingDto.getCheckSum())
            .ingestionFileSourceUri(recordingDto.getCvpFileUrl())
            .recordingSegment(recordingDto.getSegment())
            .hearingRecording(recording)
            .build();
    }

    private void snoop(final String file, final Throwable throwable) {
        final String message = String.format("An error occurred ingesting file '%s'", file);
        snooper.snoop(message, throwable);
    }

}
