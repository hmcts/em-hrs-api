package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDeletionDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingTtlMigrationDTO;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesAuditEntryRepository;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;
import uk.gov.hmcts.reform.em.hrs.service.BlobStorageDeleteService;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class HearingRecordingServiceImpl implements HearingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(HearingRecordingServiceImpl.class);
    private final HearingRecordingRepository hearingRecordingRepository;
    private final HearingRecordingSegmentRepository hearingRecordingSegmentRepository;
    private final ShareesRepository shareesRepository;
    private final HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository;
    private final HearingRecordingSegmentAuditEntryRepository hearingRecordingSegmentAuditEntryRepository;
    private final ShareesAuditEntryRepository shareesAuditEntryRepository;
    private final FolderService folderService;


    private final BlobStorageDeleteService blobStorageDeleteService;

    public HearingRecordingServiceImpl(HearingRecordingRepository hearingRecordingRepository,
                                       HearingRecordingSegmentRepository hearingRecordingSegmentRepository,
                                       ShareesRepository shareesRepository,
                                       HearingRecordingAuditEntryRepository hearingRecordingAuditEntryRepository,
                                       HearingRecordingSegmentAuditEntryRepository
                                               hearingRecordingSegmentAuditEntryRepository,
                                       ShareesAuditEntryRepository shareesAuditEntryRepository,
                                       FolderService folderService,
                                       BlobStorageDeleteService blobStorageDeleteService) {
        this.hearingRecordingRepository = hearingRecordingRepository;
        this.hearingRecordingSegmentRepository = hearingRecordingSegmentRepository;
        this.shareesRepository = shareesRepository;
        this.hearingRecordingAuditEntryRepository = hearingRecordingAuditEntryRepository;
        this.hearingRecordingSegmentAuditEntryRepository = hearingRecordingSegmentAuditEntryRepository;
        this.shareesAuditEntryRepository = shareesAuditEntryRepository;
        this.folderService = folderService;
        this.blobStorageDeleteService = blobStorageDeleteService;
    }

    @Override
    public void deleteCaseHearingRecordings(Collection<Long> ccdCaseIds) {

        try {
            List<HearingRecordingDeletionDto> hearingRecordingDtos = hearingRecordingRepository
                    .findHearingRecordingIdsAndSourceByCcdCaseIds(ccdCaseIds);

            if (CollectionUtils.isEmpty(hearingRecordingDtos)) {
                log.error("No HRS rows found for CCD Case IDs: {}", ccdCaseIds);
                return;
            }
            List<HearingRecordingDeletionDto> hearingRecordingSegmentDtos = hearingRecordingDtos.stream()
                    .flatMap(dto1 -> hearingRecordingSegmentRepository
                            .findFilenamesByHearingRecordingId(dto1.hearingRecordingId()).stream()
                            .map(dto2 -> new HearingRecordingDeletionDto(
                                    dto1.hearingRecordingId(),
                                    dto2.hearingRecordingSegmentId(),
                                    null,
                                    dto1.hearingSource(),
                                    dto2.filename())))
                    .toList();

            for (HearingRecordingDeletionDto hearingRecordingSegmentDto : hearingRecordingSegmentDtos) {
                deleteSegmentDetails(hearingRecordingSegmentDto);
            }

            List<UUID> hearingRecordingIds = hearingRecordingDtos.stream()
                    .map(HearingRecordingDeletionDto::hearingRecordingId)
                    .distinct()
                    .toList();
            List<UUID> shareeIds = shareesRepository.findAllByHearingRecordingIds(hearingRecordingIds);

            deleteShareeDetails(hearingRecordingIds,shareeIds);

            deleteHearingRecordingDetails(hearingRecordingIds);

        } catch (Exception e) {
            log.info("Database deletion failed for CCD Case IDs: {} with error: {}", ccdCaseIds, e.getMessage());
        }
    }

    @Override
    public Optional<HearingRecording> findHearingRecording(HearingRecordingDto recordingDto) {
        return hearingRecordingRepository.findByRecordingRefAndFolderName(
            recordingDto.getRecordingRef(),
            recordingDto.getFolder()
        );
    }

    @Override
    public HearingRecording createHearingRecording(HearingRecordingDto recordingDto) {
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
            return hearingRecordingRepository.saveAndFlush(recording);
        } catch (ConstraintViolationException e) {
            log.warn("Hearing Recording already exists in database.");
            throw new CcdUploadException("Hearing Recording already exists. Likely race condition from another server");
        } catch (Exception e) {
            log.warn(
                "create case Unhandled Exception whilst adding segment to DB (ref {}) to case(ccdId {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
            throw new CcdUploadException("Unhandled Exception trying to persist case");
        }
    }

    @Override
    public HearingRecording updateCcdCaseId(HearingRecording recording, Long ccdCaseId) {
        recording.setCcdCaseId(ccdCaseId);
        return hearingRecordingRepository.saveAndFlush(recording);
    }

    private void deleteHearingRecordingDetails(List<UUID> hearingRecordingIds) {
        hearingRecordingAuditEntryRepository.deleteByHearingRecordingIds(hearingRecordingIds);
        hearingRecordingRepository.deleteByHearingRecordingIds(hearingRecordingIds);
        log.info("Database deletion successful for HearingRecording with IDs: {}",
                hearingRecordingIds);
    }

    private void deleteShareeDetails(List<UUID> hearingRecordingIds, List<UUID> shareeIds) {
        shareesAuditEntryRepository.deleteByHearingRecordingShareeIds(shareeIds);
        shareesRepository.deleteByHearingRecordingIds(hearingRecordingIds);
        log.info("Database deletion successful for Sharee with HearingRecording IDs: {}",
                hearingRecordingIds);
    }

    private void deleteSegmentDetails(HearingRecordingDeletionDto hearingRecordingDeletionDto) {
        blobStorageDeleteService.deleteBlob(
                hearingRecordingDeletionDto.filename(),
                HearingSource.valueOf(hearingRecordingDeletionDto.hearingSource()));
        log.info("Blob deletion successful for filename: {} and source: {}",
                hearingRecordingDeletionDto.filename(), hearingRecordingDeletionDto.hearingSource());

        hearingRecordingSegmentAuditEntryRepository.deleteByHearingRecordingSegmentId(
                hearingRecordingDeletionDto.hearingRecordingSegmentId());
        hearingRecordingSegmentRepository.deleteById(
                hearingRecordingDeletionDto.hearingRecordingSegmentId());

        log.info("Database deletion successful for HearingRecordingSegment ID: {}",
                hearingRecordingDeletionDto.hearingRecordingSegmentId());
    }

    public Long findCcdCaseIdByFilename(String filename) {
        return hearingRecordingRepository.findCcdCaseIdByFilename(filename);
    }

    @Override
    public List<HearingRecordingTtlMigrationDTO> getRecordingsForTtlUpdate(int limit) {
        return hearingRecordingRepository.findRecordsForTtlUpdate(PageRequest.of(0, limit));
    }

    @Override
    public void updateTtl(UUID recordingId, LocalDate ttl) {
        hearingRecordingRepository.updateTtlById(recordingId, ttl);
    }
}
