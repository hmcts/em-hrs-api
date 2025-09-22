package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class QueueConfig {

    private static final int DEFAULT_CAPACITY = 1000;

    @Bean
    public LinkedBlockingQueue<HearingRecordingDto> ingestionQueue() {
        return new LinkedBlockingQueue<>(DEFAULT_CAPACITY);
    }


    @Bean
    public LinkedBlockingQueue<HearingRecordingDto> ccdUploadQueue() {
        return new LinkedBlockingQueue<>(DEFAULT_CAPACITY);
    }

}
