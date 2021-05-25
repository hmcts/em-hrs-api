package uk.gov.hmcts.reform.em.hrs.service;

import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.util.SetUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

@Named
@Transactional
public class FolderServiceImpl implements FolderService {

    private static final String FOLDER_MISSING_EXCEPTION_MSG =
        "Folders must explicitly exist, based on GET /folders/(foldername) creating them";
    private final FolderRepository folderRepository;
    private final JobInProgressRepository jobInProgressRepository;
    private final HearingRecordingStorage hearingRecordingStorage;
    private final HearingRecordingRepository hearingRecordingRepository;

    @Inject
    public FolderServiceImpl(FolderRepository folderRepository,
                             JobInProgressRepository jobInProgressRepository,
                             HearingRecordingStorage hearingRecordingStorage,
                             HearingRecordingRepository hearingRecordingRepository) {
        this.folderRepository = folderRepository;
        this.jobInProgressRepository = jobInProgressRepository;
        this.hearingRecordingStorage = hearingRecordingStorage;
        this.hearingRecordingRepository = hearingRecordingRepository;
    }

    @Override
    public Set<String> getStoredFiles(String folderName) {
        deleteStaledJobs();
        deleteStaleCcdUploadAttempts();

        Optional<Folder> optionalFolder = folderRepository.findByName(folderName);

        if (optionalFolder.isEmpty()) {
            Folder newFolder = Folder.builder().name(folderName).build();
            folderRepository.save(newFolder);
            return Collections.emptySet();
        }

        return getCompletedAndInProgressFiles(optionalFolder.get());
    }

    private void deleteStaleCcdUploadAttempts() {

        LocalDateTime yesterday = LocalDateTime.now(Clock.systemUTC()).minusHours(24);
        hearingRecordingRepository.deleteStaleRecordsWithNullCcdCaseId(yesterday);
    }

    @Override
    public Folder getFolderByName(@NotNull String folderName) {
        return folderRepository.findByName(folderName)
            .orElseThrow(() -> new DatabaseStorageException(FOLDER_MISSING_EXCEPTION_MSG));
    }


    private Set<String> getCompletedAndInProgressFiles(Folder folder) {

        Tuple2<FilesInDatabase, Set<String>> databaseRecords = getFilesetsFromDatabase(folder);
        FilesInDatabase filesInDatabase = databaseRecords.getT1();

        Set<String> filesInBlobstore = hearingRecordingStorage.findByFolder(folder.getName());

        Set<String> completedFiles = filesInDatabase.intersect(filesInBlobstore);
        Set<String> filesInProgress = databaseRecords.getT2();

        return SetUtils.union(completedFiles, filesInProgress);
    }

    private Tuple2<FilesInDatabase, Set<String>> getFilesetsFromDatabase(Folder folder) {

        Set<String> filesInDatabase = getSegmentFilenames(folder.getHearingRecordings());
        Set<String> filesInProgress = getFilesInProgress(folder.getJobsInProgress());

        return Tuples.of(new FilesInDatabase(filesInDatabase), filesInProgress);
    }

    private Set<String> getFilesInProgress(List<JobInProgress> jobInProgresses) {
        return jobInProgresses.stream()
            .map(JobInProgress::getFilename)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void deleteStaledJobs() {
        LocalDateTime yesterday = LocalDateTime.now(Clock.systemUTC()).minusHours(24);
        jobInProgressRepository.deleteByCreatedOnLessThan(yesterday);
    }

    private Set<String> getSegmentFilenames(List<HearingRecording> hearingRecordings) {
        return hearingRecordings.stream()
            .flatMap(x -> x.getSegments().stream().map(HearingRecordingSegment::getFilename))
            .collect(Collectors.toUnmodifiableSet());
    }

    static class FilesInDatabase {
        private final Set<String> fileset;

        FilesInDatabase(Set<String> fileset) {
            this.fileset = fileset;
        }

        Set<String> intersect(Set<String> filesInBlobstore) {
            return SetUtils.intersect(fileset, filesInBlobstore);
        }
    }
}
