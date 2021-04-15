package uk.gov.hmcts.reform.em.hrs.service;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

        final CompletableFuture<Void> metadataFuture = CompletableFuture.runAsync(() -> {
            LOGGER.info("request to create/update case with new hearing recording");

            final Optional<HearingRecording> optionalHearingRecording = recordingRepository.findByRecordingRef(
                hearingRecordingDto.getRecordingRef()
            );

            optionalHearingRecording
                .map(hearingRecording -> updateCase(hearingRecording, hearingRecordingDto))
                .orElse(Optional.of(createCaseinCcdAndPersist(hearingRecordingDto)))
                .ifPresent(segment -> segmentRepository.save(segment));

        });

        final CompletableFuture<Void> blobCopyFuture = CompletableFuture.runAsync(
            () -> hearingRecordingStorage.copyRecording(
                hearingRecordingDto.getCvpFileUrl(),
                hearingRecordingDto.getFilename()
            )
        );

        try {
            CompletableFuture
                .allOf(metadataFuture, blobCopyFuture)
                .get();
        } catch (
            ExecutionException e) {
            snoop(hearingRecordingDto.getCvpFileUrl(), e);
        } catch (
            InterruptedException e) {
            snoop(hearingRecordingDto.getCvpFileUrl(), e);
            Thread.currentThread().interrupt();
        }

    }

    private Optional<HearingRecordingSegment> updateCase(final HearingRecording recording,
                                                         final HearingRecordingDto recordingDto) {

        if (recording.getCcdCaseId() == null) {
            LOGGER.info(
                "Case still being created in CCD for recording ({}) to case({})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
            //TODO clean down hearingRecordings where created < yesterday and ccdID is null as part of some process
            return Optional.empty();
        }

        LOGGER.info("adding  recording ({}) to case({})", recordingDto.getRecordingRef(), recording.getCcdCaseId());

        ccdDataStoreApiClient.updateCaseData(recording.getCcdCaseId(), recording.getId(), recordingDto);

        return Optional.of(createSegment(recording, recordingDto));
    }

    private HearingRecordingSegment createCaseinCcdAndPersist(final HearingRecordingDto recordingDto) {
        LOGGER.info("creating a new case for recording: {}", recordingDto.getRecordingRef());

        Folder folder = folderService.getFolderByName(recordingDto.getFolder());

        HearingRecording recording = HearingRecording.builder()
            .folder(folder)
            .recordingRef(recordingDto.getRecordingRef())
            .caseRef(recordingDto.getCaseRef())
            .hearingLocationCode(recordingDto.getCourtLocationCode())
            .hearingRoomRef(recordingDto.getHearingRoomRef())
            .hearingSource(recordingDto.getRecordingSource())
            .jurisdictionCode(recordingDto.getJurisdictionCode())
            .serviceCode(recordingDto.getServiceCode())
            .build();

        recording = recordingRepository.save(recording);

        //create case in ccd, and update database with caseIdd
        final Long caseId = ccdDataStoreApiClient.createCase(recording.getId(), recordingDto);
        recording.setCcdCaseId(caseId);
        recording = recordingRepository.save(recording);


        return createSegment(recording, recordingDto);
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
