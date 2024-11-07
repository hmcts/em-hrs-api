package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.JobInProgress;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.DatabaseStorageException;
import uk.gov.hmcts.reform.em.hrs.repository.FolderRepository;
import uk.gov.hmcts.reform.em.hrs.repository.JobInProgressRepository;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class JobInProgressServiceImpl implements JobInProgressService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobInProgressServiceImpl.class);


    private final JobInProgressRepository jobInProgressRepository;
    private final FolderRepository folderRepository;

    @Autowired
    public JobInProgressServiceImpl(
        final JobInProgressRepository jobInProgressRepository, final FolderRepository folderRepository) {

        this.jobInProgressRepository = jobInProgressRepository;
        this.folderRepository = folderRepository;
    }

    @Override
    public void register(final HearingRecordingDto hrDto) throws DatabaseStorageException {
        String filename = hrDto.getFilename();
        String folderName = hrDto.getFolder();

        LOGGER.info("Ingestion: Registering Job In Progress for folderName/filename: {}/{}", filename, folderName);
        Optional<Folder> folder = folderRepository.findByName(folderName);
        if (folder.isEmpty()) {
            throw new DatabaseStorageException("IllegalState - Folder not found in DB: " + folderName);
        } else {
            JobInProgress job = JobInProgress
                .builder()
                .folder(folder.get())
                .filename(filename)
                .createdOn(LocalDateTime.now())
                .build();
            jobInProgressRepository.save(job);
        }
    }

    @Override
    public void deRegister(final HearingRecordingDto hrDto) {
        String filename = hrDto.getFilename();
        String folderName = hrDto.getFolder();

        LOGGER.info("Ingestion: Deregistering Job In Progress for folderName/filename: {}/{}", filename, folderName);
        Set<JobInProgress> jobInProgressSet = jobInProgressRepository.findByFolderNameAndFilename(folderName, filename);
        for (JobInProgress job : jobInProgressSet) {
            jobInProgressRepository.delete(job);
        }


    }

}
