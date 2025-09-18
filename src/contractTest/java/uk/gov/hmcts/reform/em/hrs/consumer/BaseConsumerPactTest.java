package uk.gov.hmcts.reform.em.hrs.consumer;


import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseConsumerPactTest {

    private static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    private static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";

    public static final Map<String, String> HEADERS_WITHOUT_JSON = Map.of(
        HttpHeaders.AUTHORIZATION, AUTH_TOKEN,
        "serviceauthorization", SERVICE_AUTH_TOKEN
    );

    public static final Map<String, String> HEADERS_WITH_JSON = Map.of(
        AUTHORIZATION, AUTH_TOKEN,
        "ServiceAuthorization", SERVICE_AUTH_TOKEN,
        "Content-Type", "application/json"
    );

    protected static final byte[] DOWNLOAD_CONTENT
        = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10, 0x20, 0x30, 0x40};

    protected BaseConsumerPactTest() {
    }
}
