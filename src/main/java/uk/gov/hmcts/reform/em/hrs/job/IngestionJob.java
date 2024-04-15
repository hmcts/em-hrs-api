package uk.gov.hmcts.reform.em.hrs.job;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

@Component
public class IngestionJob extends QuartzJobBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionJob.class);

    @Qualifier("ingestionQueue")
    private LinkedBlockingQueue<HearingRecordingDto> ingestionQueue;

    private JobInProgressService jobInProgressService;

    private IngestionService ingestionService;

    private LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue;


    // Required by Quartz
    public IngestionJob() {
    }

    @Autowired
    IngestionJob(@Qualifier("ingestionQueue") final LinkedBlockingQueue<HearingRecordingDto> ingestionQueue,
                 final IngestionService ingestionService, final JobInProgressService jobInProgressService,
                 @Qualifier("ccdUploadQueue") LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue) {
        this.ingestionQueue = ingestionQueue;
        this.ingestionService = ingestionService;
        this.jobInProgressService = jobInProgressService;
        this.ccdUploadQueue = ccdUploadQueue;
    }

    @Override
    protected void executeInternal(final JobExecutionContext context) {
        Optional.ofNullable(ingestionQueue.poll())
            .ifPresent(this::ingestGracefully);
    }

    private void ingestGracefully(HearingRecordingDto hrDto) {
        try {
            jobInProgressService.register(hrDto);
            ingestionService.ingest(hrDto);
            boolean accepted = ccdUploadQueue.offer(hrDto);
            if (accepted) {
                LOGGER.warn("CCD Upload Job accepted for file: {} ", hrDto.getFilename());
            } else {
                LOGGER.warn("CCD Upload Queue Full. Not uploading file: {} ", hrDto.getFilename());
                jobInProgressService.deRegister(hrDto);
            }
        } catch (RejectedExecutionException re) {
            LOGGER.warn("Execution Rejected: {}", re.toString());//likely to be timeouts with azure copies of blobstore
            jobInProgressService.deRegister(hrDto);
        } catch (Exception e) {
            LOGGER.error("Unhandled Exception: {}", e.toString());
            jobInProgressService.deRegister(hrDto);
        }
    }

}

