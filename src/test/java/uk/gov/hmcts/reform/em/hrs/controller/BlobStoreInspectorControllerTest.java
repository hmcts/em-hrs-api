package uk.gov.hmcts.reform.em.hrs.controller;

import jakarta.validation.ClockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorageImpl;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobStoreInspectorControllerTest {

    @Mock
    private HearingRecordingStorage hearingRecordingStorage;

    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private BlobStoreInspectorController blobStoreInspectorController;

    private static final String PLAIN_API_KEY = "testKey";
    private static final long FUTURE_EXPIRY = Instant.now().plusSeconds(1000).toEpochMilli();
    private static final long PAST_EXPIRY = Instant.now().minusSeconds(1000).toEpochMilli();
    private static final String VALID_TOKEN_PAYLOAD = Base64.getEncoder().encodeToString(
        (PLAIN_API_KEY + ":" + FUTURE_EXPIRY).getBytes(StandardCharsets.UTF_8)
    );
    private static final String EXPIRED_TOKEN_PAYLOAD = Base64.getEncoder().encodeToString(
        (PLAIN_API_KEY + ":" + PAST_EXPIRY).getBytes(StandardCharsets.UTF_8)
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", VALID_TOKEN_PAYLOAD);
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        lenient().when(clockProvider.getClock()).thenReturn(fixedClock);
    }

    @Test
    void inspectShouldReturnStorageReportWhenAuthIsValid() {
        StorageReport expectedReport = mock(StorageReport.class);
        when(hearingRecordingStorage.getStorageReport()).thenReturn(expectedReport);

        StorageReport result = blobStoreInspectorController.inspect("Bearer " + VALID_TOKEN_PAYLOAD);

        assertThat(result).isEqualTo(expectedReport);
    }

    @Test
    void inspectShouldThrowExceptionWhenAuthHeaderIsMissing() {
        assertThatThrownBy(() -> blobStoreInspectorController.inspect(null))
            .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("API Key is missing");
    }

    @Test
    void inspectShouldThrowExceptionWhenAuthHeaderIsEmpty() {
        assertThatThrownBy(() -> blobStoreInspectorController.inspect(""))
            .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("API Key is missing");
    }

    @Test
    void inspectShouldThrowExceptionWhenAuthKeyIsInvalid() {
        assertThatThrownBy(() -> blobStoreInspectorController.inspect("Bearer invalid-payload"))
            .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("Invalid API Key");
    }

    @Test
    void findBlobShouldReturnBlobDetailWhenAuthIsValid() {
        HearingRecordingStorageImpl.BlobDetail expectedDetail = mock(HearingRecordingStorageImpl.BlobDetail.class);
        String blobName = "test-blob";
        when(hearingRecordingStorage.findBlob(HearingSource.CVP, blobName)).thenReturn(expectedDetail);

        HearingRecordingStorageImpl.BlobDetail result = blobStoreInspectorController.findBlob(
            "Bearer " + VALID_TOKEN_PAYLOAD,
            "CVP",
            blobName
        );

        assertThat(result).isEqualTo(expectedDetail);
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenTokenDoesNotMatchConfiguredKey() {
        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer different-key");
        assertThat(result).isFalse();
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenTokenIsExpired() {
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", EXPIRED_TOKEN_PAYLOAD);
        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer " + EXPIRED_TOKEN_PAYLOAD);
        assertThat(result).isFalse();
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenTokenFormatIsInvalid() {
        String invalidFormat = Base64.getEncoder().encodeToString("no-colon-here".getBytes());
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", invalidFormat);

        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer " + invalidFormat);

        assertThat(result).isFalse();
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenExtractedKeyIsEmpty() {
        String emptyKeyFormat = Base64.getEncoder().encodeToString(String.format(":%d", FUTURE_EXPIRY).getBytes());
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", emptyKeyFormat);

        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer " + emptyKeyFormat);

        assertThat(result).isFalse();
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenBase64IsMalformed() {
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", "not-base-64-!!!");

        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer not-base-64-!!!");

        assertThat(result).isFalse();
    }

    @Test
    void isApiKeyValidShouldReturnFalseWhenTimestampIsNotLong() {
        String badTimestamp = Base64.getEncoder().encodeToString("key:not-a-number".getBytes());
        ReflectionTestUtils.setField(blobStoreInspectorController, "apiKey", badTimestamp);

        boolean result = blobStoreInspectorController.isApiKeyValid("Bearer " + badTimestamp);

        assertThat(result).isFalse();
    }
}
