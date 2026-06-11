package com.template.api.integrationtest;

import com.template.MainApplication;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.util.WireMockIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import com.template.config.security.RateLimitingFilter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class KeycloakNegativeIT extends WireMockIntegrationTest {

    KeycloakNegativeIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                       CacheManager cacheManager,
                       RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    // ------------------------------------------------------------
    // KEYCLOAK CONNECTION FAILURES
    // ------------------------------------------------------------

    @Test
    void login_keycloak_unavailable_500() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"internal_server_error\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorType.UNAUTHORIZED.code(), INVALID_GRANT);
    }

    @Test
    void login_keycloak_timeout() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(3000) // 3 seconds delay
                        .withBody("{}")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorType.INTERNAL_SERVER_ERROR.code(),
                ApiErrorType.INTERNAL_SERVER_ERROR.message());
    }

    @Test
    void login_keycloak_malformed_response() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("invalid-json")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorType.INTERNAL_SERVER_ERROR.code(),
                ApiErrorType.INTERNAL_SERVER_ERROR.message());
    }

    // ------------------------------------------------------------
    // AUTHENTICATION ERRORS
    // ------------------------------------------------------------

    @Test
    void login_invalid_credentials() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "wrongpassword",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_GRANT);
     }

    @Test
    void login_account_disabled() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Account disabled\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "disabled-user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_GRANT);
    }

    @Test
    void login_invalid_client() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_client\",\"error_description\":\"Invalid client credentials\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "wrong-client",
                "wrong-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_CLIENT);
    }

    // ------------------------------------------------------------
    // REFRESH TOKEN ERRORS
    // ------------------------------------------------------------

    @Test
    void refresh_invalid_token() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Invalid refresh token\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = refreshRequest(
                "invalid-refresh-token",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.INVALID_GRANT.code(),
                INVALID_GRANT);
    }

    @Test
    void refresh_expired_token() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Token expired\"}")));

        ResponseEntity<ApiResult<TokenResponse>> response = refreshRequest(
                "expired-refresh-token",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.INVALID_GRANT.code(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // LOGOUT/REVOKE ERRORS
    // ------------------------------------------------------------

    @Test
    void logout_invalid_token() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_LOGOUT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"invalid_grant\"}")));

        ResponseEntity<ApiResult<Void>> response = logoutRequest(
                "invalid-token",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.INVALID_TOKEN.code(),
                INVALID_GRANT);
    }

    @Test
    void logout_keycloak_unavailable() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_LOGOUT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"service_unavailable\"}")));

        ResponseEntity<ApiResult<Void>> response = logoutRequest(
                "some-token",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorType.INVALID_TOKEN.code(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // NETWORK FAILURES
    // ------------------------------------------------------------

    @Test
    void login_network_error() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorType.INTERNAL_SERVER_ERROR.code(),
                ApiErrorType.INTERNAL_SERVER_ERROR.message());
    }

    @Test
    void login_empty_response() {
        stubFor(post(urlPathMatching(REALMS_PROTOCOL_OPENID_CONNECT_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NO_CONTENT.value()))); // truly empty response

        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(
                "user",
                "password",
                "test-client",
                "test-secret"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_GRANT);
    }
}
