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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class RateLimitingIT extends WireMockIntegrationTest {

    RateLimitingIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                   CacheManager cacheManager,
                   RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    // ------------------------------------------------------------
    // LOGIN RATE LIMIT TESTS (5 requests per minute)
    // ------------------------------------------------------------

    @Test
    void login_rate_limit_blocks_after_5_requests() {

        // First 5 requests should succeed
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // 6th request should be rate limited (429 Too Many Requests)
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ApiErrorType.TOO_MANY_REQUESTS.code());
    }

    @Test
    void login_rate_limit_resets_after_20_seconds() {

        // Exhaust the rate limit (5 requests)
        for (int i = 0; i < 5; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify rate limit is active
        ResponseEntity<ApiResult<TokenResponse>> blockedResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blockedResponse.getBody()).isNotNull();
        assertThat(blockedResponse.getBody().code()).isEqualTo(ApiErrorType.TOO_MANY_REQUESTS.code());

        // Wait for rate limit to reset (20 seconds + buffer)
        await()
                .atMost(25, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(USERNAME, USER_PASSWORD);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    // ------------------------------------------------------------
    // CLIENTS RATE LIMIT TESTS (20 requests per second)
    // ------------------------------------------------------------

    @Test
    void clients_rate_limit_blocks_after_20_requests() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        List<HttpStatusCode> statuses = requestStatuses(token, 21);
        int badRequestCount = 0;
        int rateLimitedCount = 0;

        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.BAD_REQUEST)) {
                badRequestCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        assertThat(badRequestCount).as("Bad Request count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 1").isEqualTo(1);
    }

    @Test
    void clients_rate_limit_resets_after_20_seconds() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        List<HttpStatusCode> statuses = requestStatuses(token, 21);
        int badRequestCount = 0;
        int rateLimitedCount = 0;

        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.BAD_REQUEST)) {
                badRequestCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        assertThat(badRequestCount).as("Bad Request count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 1").isEqualTo(1);

        // Wait for rate limit to reset (20 seconds + buffer)
        await()
                .atMost(25, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ApiResult<Object>> response = requestGet(clientUrl + "/invalid-id", token);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void clients_rate_limit_allows_exactly_20_requests_per_second() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        int badRequestCount = 0;
        int rateLimitedCount = 0;

        List<HttpStatusCode> statuses = requestStatuses(token, 25);
        for (HttpStatusCode status : statuses) {
            if (status.equals(HttpStatus.BAD_REQUEST)) {
                badRequestCount++;
            } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                rateLimitedCount++;
            }
        }

        assertThat(badRequestCount).as("Bad Request count should be 20").isEqualTo(20);
        assertThat(rateLimitedCount).as("Rate limited count should be 5").isEqualTo(5);
    }

    // ------------------------------------------------------------
    // RATE LIMIT ISOLATION TESTS
    // ------------------------------------------------------------

    @Test
    void different_endpoints_have_independent_rate_limits() {
         String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        // Exhaust login rate limit
        for (int i = 0; i < 4; i++) {
            loginAndGetData(USERNAME, USER_PASSWORD);
        }

        // Verify login is rate limited
        ResponseEntity<ApiResult<TokenResponse>> loginResponse = loginRequest(USERNAME, USER_PASSWORD);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().code()).isEqualTo(ApiErrorType.TOO_MANY_REQUESTS.code());

        // Clients endpoint should still work (independent rate limit)
        ResponseEntity<ApiResult<Object>> response = requestGet(clientUrl + "/invalid-id", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private List<HttpStatusCode> requestStatuses(String token, int count) {
        List<CompletableFuture<HttpStatusCode>> futures = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/invalid-id", token);
                return resp.getStatusCode();
            }));
        }

        List<HttpStatusCode> statuses = new ArrayList<>(count);
        for (CompletableFuture<HttpStatusCode> future : futures) {
            try {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute clients requests", e);
            }
        }

        return statuses;
    }
}
