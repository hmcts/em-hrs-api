package uk.gov.hmcts.reform.em.hrs.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.em.hrs.util.IngestionQueue;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;

@Configuration
public class ApplicationConfig {

    @Value("${notify.apiKey}")
    private String notificationApiKey;

    @Value("${hrs.ingestion-queue-size}")
    private Integer ingestionQueueSize;

    @Bean
    public NotificationClientApi provideNotificationClient() {
        return new NotificationClient(notificationApiKey);
    }

    @Bean
    public IngestionQueue provideIngestionQueue() {
        return IngestionQueue.builder()
            .capacity(ingestionQueueSize)
            .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilder provideJackson2ObjectMapperBuilder() {
        final Jackson2ObjectMapperBuilder jsonBuilderConfig = new Jackson2ObjectMapperBuilder();
        jsonBuilderConfig.propertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        jsonBuilderConfig.findModulesViaServiceLoader(true);
        jsonBuilderConfig.failOnUnknownProperties(false);

        return jsonBuilderConfig;
    }
}
