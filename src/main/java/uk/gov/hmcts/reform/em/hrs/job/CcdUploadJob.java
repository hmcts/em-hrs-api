package uk.gov.hmcts.reform.em.hrs.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.JobInProgressService;
import uk.gov.hmcts.reform.em.hrs.service.ccd.CcdUploadService;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;


@Component
@DisallowConcurrentExecution
public class CcdUploadJob extends QuartzJobBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcdUploadJob.class);

    private LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue;

    private JobInProgressService jobInProgressService;

    private CcdUploadService ccdUploadService;

    // Required by Quartz
    public CcdUploadJob() {
    }

    @Autowired
    CcdUploadJob(@Qualifier("ccdUploadQueue") final LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue,
                 final CcdUploadService ccdUploadService,
                 final JobInProgressService jobInProgressService) {
        this.ccdUploadQueue = ccdUploadQueue;
        this.ccdUploadService = ccdUploadService;
        this.jobInProgressService = jobInProgressService;
    }

    @Override
    protected void executeInternal(final JobExecutionContext context) {
        LOGGER.info("CCD upload job starting");
        Optional.ofNullable(ccdUploadQueue.poll())
            .ifPresent(this::uploadGracefully);
    }

    private void uploadGracefully(HearingRecordingDto hrDto) {
        LOGGER.info("attempting to create/update case in ccd gracefully");
        try {
            ccdUploadService.upload(hrDto);
        } catch (Exception e) {
            LOGGER.error("Unhandled Exception: {}", e.toString());
        }
        jobInProgressService.deRegister(hrDto);
    }
}

