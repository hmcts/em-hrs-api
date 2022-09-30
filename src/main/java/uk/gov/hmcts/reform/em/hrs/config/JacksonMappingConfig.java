package uk.gov.hmcts.reform.em.hrs.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonMappingConfig {

    @Bean
    public Jackson2ObjectMapperBuilder provideJackson2ObjectMapperBuilder() {
        final Jackson2ObjectMapperBuilder jsonBuilderConfig = new Jackson2ObjectMapperBuilder();
        jsonBuilderConfig.propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        jsonBuilderConfig.findModulesViaServiceLoader(true);
        jsonBuilderConfig.failOnUnknownProperties(false);
        jsonBuilderConfig.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return jsonBuilderConfig;
    }


}
