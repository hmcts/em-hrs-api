package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.em.hrs.dto.HearingSource;
import uk.gov.hmcts.reform.em.hrs.helper.TestClockProvider;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorageImpl;
import uk.gov.hmcts.reform.em.hrs.storage.StorageReport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.hrs.config.ClockConfig.EUROPE_LONDON_ZONE_ID;

@WebMvcTest(BlobStoreInspectorController.class)
@Import(TestClockProvider.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BlobStoreInspectorControllerTest extends BaseWebTest {

    private static final String REPORT_URI = "/report";
    private static final String FIND_BLOB_URI_TEMPLATE = "/report/hrs/CVP/%s";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_FIELD = "apiKey";
    private static final String EXPIRED_DUMMY_KEY = "RkI2ejoxNjk1OTA2MjM0MDcx";

    @MockitoBean
    private HearingRecordingStorage hearingRecordingStorage;

    private BlobStoreInspectorController blobStoreInspectorController;

    @Autowired
    public BlobStoreInspectorControllerTest(
        WebApplicationContext context,
        BlobStoreInspectorController blobStoreInspectorController
    ) {
        super(context);
        this.blobStoreInspectorController = blobStoreInspectorController;
    }

    @BeforeEach
    @Override
    @SuppressWarnings("java:S2696") // Need to set static field from instance method for test setup
    public void setup() {
        super.setup();
        TestClockProvider.stoppedInstant = Instant.now();
    }

    private String generateFutureApiKey() {
        long futureTimestamp = TestClockProvider.stoppedInstant.plus(1, ChronoUnit.DAYS).toEpochMilli();
        String decodedKey = "test-key:" + futureTimestamp;
        return Base64.getEncoder().encodeToString(decodedKey.getBytes());
    }

    @Test
    public void inspectEndpointReturnsOkResponseWithValidApiKey() throws Exception {
        var today = LocalDate.ofInstant(TestClockProvider.stoppedInstant, EUROPE_LONDON_ZONE_ID);
        var storageReport = new StorageReport(
            today,
            new StorageReport.HrsSourceVsDestinationCounts(1567, 1232, 332, 1000)
        );
        String validApiKey = generateFutureApiKey();
        ReflectionTestUtils.setField(blobStoreInspectorController, API_KEY_FIELD, validApiKey);

        when(hearingRecordingStorage.getStorageReport()).thenReturn(storageReport);

        mockMvc.perform(get(REPORT_URI)
                            .header(AUTHORIZATION, BEARER_PREFIX + validApiKey))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value(today.toString()))
            .andExpect(jsonPath("$.cvp-item-count").value(1567))
            .andExpect(jsonPath("$.hrs-cvp-item-count").value(1232))
            .andExpect(jsonPath("$.cvp-item-count-today").value(332))
            .andExpect(jsonPath("$.hrs-cvp-item-count-today").value(1000));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("unauthorizedReportScenarios")
    public void inspectEndpointReturns401ForInvalidApiKey(
        String testCaseName,
        @Nullable String authHeader,
        Consumer<BlobStoreInspectorController> setupAction
    ) throws Exception {

        setupAction.accept(blobStoreInspectorController);

        var requestBuilder = get(REPORT_URI);
        if (authHeader != null) {
            requestBuilder.header(AUTHORIZATION, authHeader);
        }

        mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> unauthorizedReportScenarios() {
        Consumer<BlobStoreInspectorController> noop = controller -> {};
        String malformedKey = Base64.getEncoder().encodeToString("malformed-key".getBytes());
        String nonBase64Key = "this-is-not-base64";
        long farFutureTimestamp = 4102444800000L;
        String keyWithEmptyPart = Base64.getEncoder().encodeToString((":" + farFutureTimestamp).getBytes());

        return Stream.of(
            Arguments.of(Named.of("when Authorization header is missing",
                                  "when Authorization header is missing"), null, noop),
            Arguments.of(Named.of("when API key is invalid",
                                  "when API key is invalid"), BEARER_PREFIX + "invalid-key", noop),
            Arguments.of(Named.of("when API key has expired",
                                  "when API key has expired"), BEARER_PREFIX + EXPIRED_DUMMY_KEY, noop),
            Arguments.of(
                Named.of("when configured API key is poorly formatted",
                         "when configured API key is poorly formatted"),
                BEARER_PREFIX + malformedKey,
                (Consumer<BlobStoreInspectorController>) controller ->
                    ReflectionTestUtils.setField(controller, API_KEY_FIELD, malformedKey)
            ),
            Arguments.of(
                Named.of("when configured API key is not Base64", "when configured API key is not Base64"),
                BEARER_PREFIX + nonBase64Key,
                (Consumer<BlobStoreInspectorController>) controller ->
                    ReflectionTestUtils.setField(controller, API_KEY_FIELD, nonBase64Key)
            ),
            Arguments.of(
                Named.of("when configured API key has an empty part", "when configured API key has an empty part"),
                BEARER_PREFIX + keyWithEmptyPart,
                (Consumer<BlobStoreInspectorController>) controller ->
                    ReflectionTestUtils.setField(controller, API_KEY_FIELD, keyWithEmptyPart)
            )
        );
    }

    @Test
    public void findBlobEndpointReturnsOkResponseWithValidApiKey() throws Exception {
        String blobName = UUID.randomUUID() + ".txt";
        String validApiKey = generateFutureApiKey();
        ReflectionTestUtils.setField(blobStoreInspectorController, API_KEY_FIELD, validApiKey);

        OffsetDateTime time = OffsetDateTime.ofInstant(TestClockProvider.stoppedInstant, EUROPE_LONDON_ZONE_ID)
            .truncatedTo(ChronoUnit.SECONDS);
        String blobUrl = "http://cvp.blob/" + blobName;
        when(hearingRecordingStorage.findBlob(HearingSource.CVP, blobName)).thenReturn(
            new HearingRecordingStorageImpl.BlobDetail(blobUrl, 10, time)
        );

        mockMvc.perform(get(String.format(FIND_BLOB_URI_TEMPLATE, blobName))
                            .header(AUTHORIZATION, BEARER_PREFIX + validApiKey))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.blob-url").value(blobUrl))
            .andExpect(jsonPath("$.blob-size").value(10))
            .andExpect(jsonPath("$.last-modified").value(time.toString()));
    }

    @ParameterizedTest(name = "{index} => Unauthorized with header: {0}")
    @MethodSource("unauthorizedFindBlobScenarios")
    public void findBlobEndpointReturns401ForInvalidApiKey(@Nullable String authHeader) throws Exception {
        String blobName = UUID.randomUUID() + ".txt";
        var requestBuilder = get(String.format(FIND_BLOB_URI_TEMPLATE, blobName));

        if (authHeader != null) {
            requestBuilder.header(AUTHORIZATION, authHeader);
        }

        mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> unauthorizedFindBlobScenarios() {
        return Stream.of(
            Arguments.of((Object) null),
            Arguments.of(BEARER_PREFIX + "invalid"),
            Arguments.of(BEARER_PREFIX + EXPIRED_DUMMY_KEY)
        );
    }
}
