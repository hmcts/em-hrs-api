package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.validation.ClockProvider;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorageImpl;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class BlobStoreInspectorController {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreInspectorController.class);

    private final HearingRecordingStorage hearingRecordingStorage;
    @Value("${report.api-key}")
    private String apiKey;

    private final ClockProvider clockProvider;

    @Autowired
    public BlobStoreInspectorController(HearingRecordingStorage hearingRecordingStorage, ClockProvider clockProvider) {
        this.hearingRecordingStorage = hearingRecordingStorage;
        this.clockProvider = clockProvider;
    }

    @GetMapping(value = "/report", consumes = MediaType.ALL_VALUE)
    public StorageReport inspect(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader
    ) {
        validateAuthorization(authHeader);
        log.info("BlobStoreInspector Controller");
        return hearingRecordingStorage.getStorageReport();
    }

    @GetMapping(value = "/report/hrs/{hearingSourceStr}/{blobName}", consumes = MediaType.ALL_VALUE)
    public HearingRecordingStorageImpl.BlobDetail findBlob(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable @Pattern(regexp = "^(VH|CVP)$", message = "Container must be 'VH' or 'CVP'")
        String hearingSourceStr,
        @PathVariable String blobName
    ) {
        validateAuthorization(authHeader);
        log.info("BlobStoreInspector Controller");
        return hearingRecordingStorage.findBlob(HearingSource.valueOf(hearingSourceStr), blobName);
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
            String[] parts = decodedString.split(":");
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
