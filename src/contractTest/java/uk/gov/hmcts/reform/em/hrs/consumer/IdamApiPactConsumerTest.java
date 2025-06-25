package uk.gov.hmcts.reform.em.hrs.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("contract")
@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(pactVersion = PactSpecVersion.V3)
public class IdamApiPactConsumerTest {

    private static final String IDAM_OPENID_TOKEN_URL = "/o/token";
    private static final String IDAM_DETAILS_URL = "/o/userinfo";
    private static final String ROLES = "roles";
    private static final String PASSWORD = "password";
    private static final String SCOPE = "scope";
    private static final String PASSWORD_VALUE = "Password123";
    private static final String EM_CASE_OFFICER = "emCaseOfficer@email.net";

    @Pact(provider = "Idam_api", consumer = "hrs_api")
    public RequestResponsePact executeGetIdamAccessTokenAndGet200(PactDslWithProvider builder) throws JSONException {
        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.put("email", EM_CASE_OFFICER);
        params.put(PASSWORD, PASSWORD_VALUE);
        params.put("forename", "emCaseOfficer");
        params.put("surname", "jar123");

        List<String> rolesList = List.of("citizen");
        params.put(ROLES, rolesList);

        return builder
            .given("a user exists", params)
            .uponReceiving("Provider takes user/pwd and returns Access Token to Hrs API")
            .path(IDAM_OPENID_TOKEN_URL)
            .method(HttpMethod.POST.toString())
            .body(
                "redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback&client_id=pact"
                    + "&grant_type=password&username=emCaseOfficer%40email.net"
                    + "&password=Password123&client_secret=pactsecret"
                    + "&scope=openid profile roles",
                "application/x-www-form-urlencoded")
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseheaders)
            .body(createAuthResponse())
            .toPact();
    }

    @PactTestFor(pactMethod = "executeGetIdamAccessTokenAndGet200")
    @Test
    public void should_post_to_token_endpoint_and_receive_access_token_with_200_response(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            RestAssured
                .given()
                .contentType(ContentType.URLENC)
                .formParam("redirect_uri", "http://www.dummy-pact-service.com/callback")
                .formParam("client_id", "pact")
                .formParam("grant_type", PASSWORD)
                .formParam("username", EM_CASE_OFFICER)
                .formParam(PASSWORD, PASSWORD_VALUE)
                .formParam("client_secret", "pactsecret")
                .formParam(SCOPE, "openid profile roles")
                .post(mockServer.getUrl() + IDAM_OPENID_TOKEN_URL)
                .then()
                .log().all().extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response.getString("access_token")).isNotBlank();

    }

    @Pact(provider = "Idam_api", consumer = "hrs_api")
    public RequestResponsePact executeGetUserInfoAndGet200(PactDslWithProvider builder) {

        Map<String, String> requestHeaders = Maps.newHashMap();
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        params.put("redirect_uri", "http://www.dummy-pact-service.com/callback");
        params.put("client_id", "pact");
        params.put("client_secret", "pactsecret");
        params.put(SCOPE, "openid profile roles");
        params.put("username", EM_CASE_OFFICER);
        params.put(PASSWORD, PASSWORD_VALUE);

        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/json");

        return builder
            .given("I have obtained an access_token as a user", params)
            .uponReceiving("Provider returns user info to Annotation API")
            .path(IDAM_DETAILS_URL)
            .headers("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseHeaders)
            .body(createUserInfoResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetUserInfoAndGet200")
    public void should_get_user_info_with_access_token(MockServer mockServer) throws JSONException {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre");
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String detailsResponseBody =
            RestAssured
                .given()
                .headers(headers)
                .when()
                .get(mockServer.getUrl() + IDAM_DETAILS_URL)
                .then()
                .statusCode(200)
                .and()
                .extract()
                .body()
                .asString();

        JSONObject response = new JSONObject(detailsResponseBody);

        assertThat(detailsResponseBody).isNotNull();
        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(response.getString("uid")).isNotBlank();
        JSONArray rolesArr = response.getJSONArray(ROLES);
        assertThat(rolesArr).isNotNull();
        assertThat(rolesArr.length()).isNotZero();
        assertThat(rolesArr.get(0).toString()).isNotBlank();

    }

    private PactDslJsonBody createAuthResponse() {

        return new PactDslJsonBody()
            .stringType("access_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("refresh_token", "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92V")
            .stringType(SCOPE, "openid roles profile")
            .stringType("id_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("token_type", "Bearer")
            .stringType("expires_in", "28798");
    }

    private DslPart createUserInfoResponse() {
        return new PactDslJsonBody()
            .stringType("uid", "1234-2345-3456-4567")
            .array(ROLES)
            .stringType("citizen")
            .closeArray();

    }
}
