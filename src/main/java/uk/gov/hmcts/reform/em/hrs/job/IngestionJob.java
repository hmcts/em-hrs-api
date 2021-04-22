package uk.gov.hmcts.reform.em.hrs.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.util.IngestionQueue;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IngestionJob extends QuartzJobBean {
    @Inject
    private IngestionQueue ingestionQueue;

    @Inject
    private IngestionService ingestionService;

    // Required by Quartz
    public IngestionJob() {
    }

    IngestionJob(final IngestionQueue ingestionQueue, final IngestionService ingestionService) {
        this.ingestionQueue = ingestionQueue;
        this.ingestionService = ingestionService;
    }

    @Override
    protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {
        Optional.ofNullable(ingestionQueue.poll())
            .ifPresent(x -> ingestionService.ingest(x));
    }
}
