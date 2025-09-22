package uk.gov.hmcts.reform.em.hrs.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TTLPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource)
        throws IOException {
        TypeReference<Map<String,Map<String, Period>>> typeRef = new TypeReference<>() {};
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        Map<String, Map<String, Period>> jsonContent = objectMapper
            .readValue(resource.getInputStream(), typeRef);
        return new MapPropertySource("json-property" + UUID.randomUUID(), new HashMap<>(jsonContent));
    }
}
