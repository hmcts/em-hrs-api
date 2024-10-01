package uk.gov.hmcts.reform.em.hrs.service;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.util.SetUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Transactional
public class FolderServiceImpl implements FolderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderServiceImpl.class);

    private static final String FOLDER_MISSING_EXCEPTION_MSG =
        "Folders must explicitly exist, based on GET /folders/(foldername) creating them";
    private final FolderRepository folderRepository;
    private final HearingRecordingSegmentRepository hearingRecordingSegmentRepository;

    @Autowired
    public FolderServiceImpl(
        FolderRepository folderRepository,
        HearingRecordingSegmentRepository hearingRecordingSegmentRepository
    ) {
        this.folderRepository = folderRepository;
        this.hearingRecordingSegmentRepository = hearingRecordingSegmentRepository;
    }

    @Override
    public Set<String> getStoredFiles(String folderName) {

        Optional<Folder> optionalFolder = folderRepository.findByName(folderName);

        if (optionalFolder.isEmpty()) {
            Folder newFolder = Folder.builder().name(folderName).build();
            folderRepository.save(newFolder);
            return Collections.emptySet();
        }

        return getCompletedAndInProgressFiles(optionalFolder.get());
    }

    @Override
    public Folder getFolderByName(@NotNull String folderName) {
        return folderRepository.findByName(folderName)
            .orElseThrow(() -> new DatabaseStorageException(FOLDER_MISSING_EXCEPTION_MSG));
    }

    private Set<String> getCompletedAndInProgressFiles(Folder folder) {

        Tuple2<FilesInDatabase, Set<String>> databaseRecords = getFilesetsFromDatabase(folder);
        FilesInDatabase filesInDatabase = databaseRecords.getT1();
        LOGGER.debug("Files In Database folder={}, {}", folder.getName(), filesInDatabase);

        Set<String> completedFiles = filesInDatabase.fileset;
        LOGGER.debug("Completed Files={}", completedFiles);

        Set<String> filesInProgress = databaseRecords.getT2();
        LOGGER.debug("FilesInProgress {}", filesInProgress);

        return SetUtils.union(completedFiles, filesInProgress);
    }

    private Tuple2<FilesInDatabase, Set<String>> getFilesetsFromDatabase(Folder folder) {

        Set<String> filesInDatabase = getSegmentFilenamesInFolder(folder.getName());
        LOGGER.debug("Files In Database {} ", filesInDatabase);
        Set<String> filesInProgress = getFilesInProgress(folder.getJobsInProgress());
        LOGGER.debug("Files In Progress {}", filesInProgress);
        return Tuples.of(new FilesInDatabase(filesInDatabase), filesInProgress);
    }

    private Set<String> getFilesInProgress(List<JobInProgress> jobInProgresses) {
        return jobInProgresses.stream()
            .map(JobInProgress::getFilename)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> getSegmentFilenamesInFolder(String folderName) {
        Set<HearingRecordingSegment> segments =
            hearingRecordingSegmentRepository.findByHearingRecordingFolderName(folderName);
        return segments.stream()
            .map(HearingRecordingSegment::getFilename)
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
