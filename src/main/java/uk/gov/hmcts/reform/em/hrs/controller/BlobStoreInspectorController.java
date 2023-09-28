package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.validation.ClockProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class BlobStoreInspectorController {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreInspectorController.class);

    @Autowired
    private HearingRecordingStorage hearingRecordingStorage;
    @Value("${report.api-key}")
    private String apiKey;

    @Autowired
    private ClockProvider clockProvider;

    @GetMapping(value = "/report", consumes = MediaType.ALL_VALUE)
    public StorageReport inspect(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader
    ) {
        validateAuthorization(authHeader);
        log.info("BlobStoreInspector Controller");
        return hearingRecordingStorage.getStorageReport();
    }

    private void validateAuthorization(String authorizationKey) {

        if (StringUtils.isEmpty(authorizationKey)) {
            log.error("API Key is missing");
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!isApiKeyValid(authorizationKey)) {
            log.error("Invalid API Key");
            throw new InvalidApiKeyException("Invalid API Key");
        }

    }

    public boolean isApiKeyValid(String authorizationKey) {
        try {
            if (!authorizationKey.equals("Bearer " + this.apiKey)) {
                return false;
            }
            String receivedApiKey = authorizationKey.replace("Bearer ", "");
            byte[] decodedBytes = Base64.getDecoder().decode(receivedApiKey);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            System.out.println(decodedString);
            String[] parts = decodedString.split(":");
            System.out.println(parts[1]);
            if (parts.length == 2) {
                String extractedApiKey = parts[0];
                long expirationTimeMillis = Long.parseLong(parts[1]);
                long currentTimeMillis = Instant.now(clockProvider.getClock()).toEpochMilli();
                return currentTimeMillis <= expirationTimeMillis && !extractedApiKey.isEmpty();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
