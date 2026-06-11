package com.template.api.integrationtest;

import com.template.MainApplication;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.util.KeycloakIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import com.template.config.security.RateLimitingFilter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class KeycloakIntegrationIT extends KeycloakIntegrationTest {

    KeycloakIntegrationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                          CacheManager cacheManager,
                          RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    // ------------------------------------------------------------
    // LOGIN TESTS
    // ------------------------------------------------------------

    @Test
    void login_success() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse data = assertStatusAndBodyAndReturnBody(response, TokenResponse.class);
        assertThat(data.accessToken()).as("Access token should not be blank").isNotBlank();

        String refreshCookie = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElse(null);
        assertThat(refreshCookie).as("Refresh token cookie should be set").isNotBlank();
    }

    @Test
    void login_wrong_password() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(USERNAME, "wrongpassword");

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_GRANT);
    }

    @Test
    void login_unknown_user() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest("unknownuser", "whatever");

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // ADMIN LOGIN TEST
    // ------------------------------------------------------------

    @Test
    void admin_login_success() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(ADMIN, ADMIN_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse data = assertStatusAndBodyAndReturnBody(response, TokenResponse.class);
        assertThat(data.accessToken()).as("Access token should not be blank").isNotBlank();
    }

    // ------------------------------------------------------------
    // REFRESH TOKEN TESTS
    // ------------------------------------------------------------

    @Test
    void refresh_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<TokenResponse>> refreshResponse = refreshRequest(refreshToken);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse refreshData = assertStatusAndBodyAndReturnBody(refreshResponse, TokenResponse.class);
        assertThat(refreshData.accessToken()).as("Access token should not be blank").isNotBlank();

        String newRefreshCookie = refreshResponse.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElse(null);
        assertThat(newRefreshCookie).as("New refresh token cookie should be set").isNotBlank();
    }

    @Test
    void refresh_wrong_token() {
        ResponseEntity<ApiResult<TokenResponse>> refreshResponse = refreshRequest("invalid-token");

        assertErrorStatusAndBody(refreshResponse, HttpStatus.BAD_REQUEST,
                ApiErrorType.INVALID_GRANT.code(),
                INVALID_GRANT);
    }

    // ------------------------------------------------------------
    // LOGOUT TESTS
    // ------------------------------------------------------------

    @Test
    void logout_success() {
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Void>> logoutResponse = logoutRequest(refreshToken);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_wrong_token() {
        ResponseEntity<ApiResult<Void>> response = logoutRequest("invalid-token");

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.INVALID_TOKEN.code(),
                INVALID_GRANT);
    }
}
