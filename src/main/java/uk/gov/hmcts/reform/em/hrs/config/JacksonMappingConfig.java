package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 mapper for Boot 4.
 *
 * <ul>
 *   <li>Primary / MVC: kebab-case {@link JsonMapper} via {@link JsonMapperBuilderCustomizer}
 *       (ingest DTO and folder/report API contracts).</li>
 *   <li>CCD: dedicated {@code ccdObjectMapper} without a naming strategy — Case* models use
 *       explicit {@code @JsonProperty} (camelCase / snake_case / PascalCase) and must not inherit
 *       MVC kebab-case.</li>
 *   <li>Azure SDK / Feign-jackson may still pull Jackson 2 transitively (SDK-internal bridge);
 *       application MVC and CCD conversion paths use Jackson 3 only.</li>
 * </ul>
 */
@Configuration
public class JacksonMappingConfig {

    public static final String CCD_OBJECT_MAPPER = "ccdObjectMapper";

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            builder.propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // Boot 3 / Jackson 2 defaulted missing primitives; Jackson 3 fails unless aligned.
            builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
            builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }

    /**
     * Jackson 3 mapper for CCD case-data convertValue paths. Not the MVC preferred mapper.
     */
    @Bean(name = CCD_OBJECT_MAPPER)
    @Qualifier(CCD_OBJECT_MAPPER)
    public ObjectMapper ccdObjectMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }
}
