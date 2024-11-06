package uk.gov.hmcts.reform.em.hrs.batch;

import jakarta.persistence.EntityManagerFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;

import java.util.Date;
import javax.sql.DataSource;

@EnableBatchProcessing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@Configuration
@ConditionalOnProperty("spring.batch.jurisdictionCodes.enabled")
public class UpdateJurisdictionCodesConfiguration {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final JobLauncher jobLauncher;

    private final EntityManagerFactory entityManagerFactory;

    private final UpdateJurisdictionCodesProcessor updateJurisdictionCodesProcessor;

    @Value("${spring.batch.jurisdictionCodes.pageSize}")
    private int jurisdictionCodesPageSize;

    @Value("${spring.batch.jurisdictionCodes.maxItemCount}")
    private int jurisdictionCodesItemCount;

    @Value("${spring.batch.jurisdictionCodes.chunkSize}")
    private int jurisdictionCodesChunkSize;

    @Autowired
    public UpdateJurisdictionCodesConfiguration(JobRepository jobRepository,
                                                PlatformTransactionManager transactionManager, JobLauncher jobLauncher,
                                                EntityManagerFactory entityManagerFactory,
                                                UpdateJurisdictionCodesProcessor updateJurisdictionCodesProcessor) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobLauncher = jobLauncher;
        this.entityManagerFactory = entityManagerFactory;
        this.updateJurisdictionCodesProcessor = updateJurisdictionCodesProcessor;
    }

    @Scheduled(cron = "${spring.batch.jurisdictionCodes.cronJobSchedule}")
    @SchedulerLock(name = "jurisdictionCodesUpdate")
    public void scheduleJurisdictionCodesUpdate() throws JobParametersInvalidException,
        JobExecutionAlreadyRunningException,
        JobRestartException,
        JobInstanceAlreadyCompleteException {

        jobLauncher.run(updateJurisdictionCodesJob(), new JobParametersBuilder()
            .addDate("date", new Date())
            .toJobParameters());
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    public Job updateJurisdictionCodesJob() {
        return new JobBuilder("updateJurisdictionCodesJob", jobRepository)
            .flow(new StepBuilder("updateJurisdictionCodesStep",jobRepository)
                      .<HearingRecordingSegment, HearingRecording>chunk(jurisdictionCodesChunkSize, transactionManager)
                      .reader(buildMissingCodesReader())
                      .processor(updateJurisdictionCodesProcessor)
                      .writer(itemWriter())
                      .build()).build().build();
    }

    private JpaPagingItemReader<HearingRecordingSegment> buildMissingCodesReader() {
        return new JpaPagingItemReaderBuilder<HearingRecordingSegment>()
            .name("documentTaskReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("select hrs from HearingRecordingSegment hrs where hrs.hearingRecording in "
                             + "(select hr from HearingRecording hr where hr.serviceCode is null and "
                             + "hr.jurisdictionCode is null order by createdOn asc)")
            .pageSize(jurisdictionCodesPageSize)
            .maxItemCount(jurisdictionCodesItemCount)
            .build();
    }

    private <T> JpaItemWriter<T> itemWriter() {
        JpaItemWriter<T> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
