package uk.gov.hmcts.reform.em.hrs.job;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class JobOrchestrator {
    private final Scheduler scheduler;
    private final int rate;

    @Inject
    public JobOrchestrator(final Scheduler scheduler, @Value("${hrs.ingestion-frequency}") final int rate) {
        this.scheduler = scheduler;
        this.rate = rate;
    }

    @PostConstruct
    public void start() throws SchedulerException {
        final String nameElement = "CVP-Hearing-Recording";
        final String groupElement = "HRS-Ingestion-Jobs";
        final JobDetail jobDetail = JobBuilder.newJob(IngestionJob.class)
            .withIdentity(nameElement, groupElement)
            .withDescription("Ingests hearing recordings from CVP into HRS")
            .build();

        final Trigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(rate)
                    .withMisfireHandlingInstructionIgnoreMisfires()
                    .repeatForever()
            ).build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    @PreDestroy
    public void stop() throws SchedulerException {
        scheduler.shutdown(true);
    }
}
