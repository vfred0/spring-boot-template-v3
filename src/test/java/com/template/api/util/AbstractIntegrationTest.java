package com.template.api.util;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.auth.SignInRequest;
import com.template.api.dtos.auth.SignOutRequest;
import com.template.api.dtos.auth.RefreshRequest;
import com.template.api.dtos.auth.TokenResponse;
import com.template.config.security.RateLimitingFilter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class AbstractIntegrationTest {

    // Testcontainers-managed shared containers for integration tests
    private static final String DB_NAME = "appdb";
    private static final String DB_USER = "app";
    private static final String DB_PASS = "app";
    public static final String RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED = "Response message should match expected";
    public static final String RESPONSE_BODY_SHOULD_NOT_BE_NULL = "Response body should not be null";
    public static final String RESPONSE_DATA_SHOULD_NOT_BE_NULL = "Response data should not be null";
    public static final String RESPONSE_HTTP_STATUS_SHOULD_BE_OK = "Response HTTP status should be OK";
    public static final String RESPONSE_CODE_SHOULD_BE_ZERO = "Response code should be zero";
    public static final String RESPONSE_HTTP_STATUS_SHOULD_MATCH_EXPECTED = "Response HTTP status should match expected";
    public static final String RESPONSE_CODE_SHOULD_MATCH_EXPECTED = "Response code should match expected";
    private static final String RESPONSE_MESSAGE_SHOULD_NOT_BE_NULL = "Response message should not be null";
    public static final String FILTER_CONFIG_CACHE = "filterConfigCache";

    @Container
    protected static PostgreSQLContainer pg = new PostgreSQLContainer("postgres:16")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASS)
            .withReuse(true);

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        if (!pg.isRunning()) {
            pg.start();
        }
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
        registry.add("spring.flyway.url", pg::getJdbcUrl);
        registry.add("spring.flyway.user", pg::getUsername);
        registry.add("spring.flyway.password", pg::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @BeforeAll
    static void checkDocker() {
        assumeTrue(pg != null && pg.isRunning(), "Postgres container must be running");
    }

    protected final KeycloakProperties props;

    protected RestTemplate restTemplate;

    protected final CacheManager cacheManager;

    protected final RateLimitingFilter rateLimitingFilter;

    protected AbstractIntegrationTest(KeycloakProperties props,
                                      CacheManager cacheManager,
                                      RateLimitingFilter rateLimitingFilter) {
        this.props = props;
        this.cacheManager = cacheManager;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    // Use a local ObjectMapper in Boot 4 PoC so integration tests do not depend on a Spring-managed bean.
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    protected static final String USERNAME = "user";
    protected static final String USER_PASSWORD = "password";
    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PASSWORD = "admin";
    protected static final String INVALID_GRANT = "invalid_grant";
    protected static final String INVALID_CLIENT = "invalid_client";

    @LocalServerPort
    protected int port;

    protected String mainUrl;

    protected String loginUrl;
    protected String refreshUrl;
    protected String logoutUrl;

    protected String clientUrl;
    protected String accountUrl;
    protected String requestUrl;

    @BeforeEach
    void initializeUrls() {
        ensureSchemaMigrated();

        mainUrl = "http://localhost:" + port + "/api";
        loginUrl = mainUrl + "/auth/login";
        refreshUrl = mainUrl + "/auth/refresh";
        logoutUrl = mainUrl + "/auth/logout";

        clientUrl = mainUrl + "/clients";
        accountUrl = mainUrl + "/accounts";
        requestUrl = mainUrl + "/requests";
        restTemplate = createTestRestTemplate();

        if (rateLimitingFilter != null) {
            rateLimitingFilter.clearBuckets();
        }

        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                if (FILTER_CONFIG_CACHE.equals(cacheName)) {
                    continue;
                }

                clearCache(cacheName);
            }
        }
    }

    protected void clearCache(String cacheName) {
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void ensureSchemaMigrated() {
        Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private RestTemplate createTestRestTemplate() {
        RestTemplate template = new RestTemplate(
                new HttpComponentsClientHttpRequestFactory(
                        HttpClients.custom()
                                .disableAutomaticRetries()
                                .build()
                )
        );
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
        return template;
    }

    protected <T> void assertErrorBody(ResponseEntity<ApiResult<T>> response, int expectedCode, Object expectedMessage) {
        ApiResult<T> body = response.getBody();
        assertThat(body).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();
        assertThat(body.code()).as(RESPONSE_CODE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedCode);
        if (expectedMessage instanceof String) {
            assertThat(body.message()).as(RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedMessage);
        } else if (expectedMessage instanceof Set<?>) {
            String msg = body.message();
            assertThat(msg).as(RESPONSE_MESSAGE_SHOULD_NOT_BE_NULL).isNotNull();
            Set<String> actual = Arrays.stream(msg.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            assertThat(actual).as(RESPONSE_MESSAGE_SHOULD_MATCH_EXPECTED).isEqualTo(expectedMessage);
        }
    }

    protected <T> void assertErrorStatusAndBody(ResponseEntity<ApiResult<T>> response,
                                            HttpStatus expectedStatus,
                                            int expectedCode,
                                            Object expectedMessage) {
        assertThat(response.getStatusCode()).as(RESPONSE_HTTP_STATUS_SHOULD_MATCH_EXPECTED).isEqualTo(expectedStatus);
        assertErrorBody(response, expectedCode, expectedMessage);
    }

    // Make this method fully generic and return T to avoid unchecked casts in tests
    protected <T> T assertStatusAndBodyAndReturnBody(ResponseEntity<ApiResult<T>> response,
                                                    HttpStatus expectedStatus,
                                                    Class<T> clazz) {
        assertThat(response.getStatusCode()).as(RESPONSE_HTTP_STATUS_SHOULD_BE_OK).isEqualTo(expectedStatus);
        assertThat(response.getBody()).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();

        ApiResult<T> api = response.getBody();
        assertThat(api).as(RESPONSE_BODY_SHOULD_NOT_BE_NULL).isNotNull();
        assertThat(api.code()).as(RESPONSE_CODE_SHOULD_BE_ZERO).isEqualTo(0);

        Object raw = api.data();
        assertThat(raw).as(RESPONSE_DATA_SHOULD_NOT_BE_NULL).isNotNull();

        T data = objectMapper.convertValue(raw, clazz);
        assertThat(data).as(RESPONSE_DATA_SHOULD_NOT_BE_NULL).isNotNull();

        return data;
    }

    protected <T> T assertStatusAndBodyAndReturnBody(ResponseEntity<ApiResult<T>> response, Class<T> clazz) {
        return assertStatusAndBodyAndReturnBody(response, HttpStatus.OK, clazz);
    }

    private HttpHeaders createHeaders(String token, String acceptLanguage, String dpopProof, boolean dpopScheme) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        if (token != null && !token.isEmpty()) {
            if (dpopScheme) {
                headers.set(HttpHeaders.AUTHORIZATION, "DPoP " + token);
            } else {
                headers.setBearerAuth(token);
            }
        }
        if (dpopProof != null && !dpopProof.isEmpty()) {
            headers.set("DPoP", dpopProof);
        }
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            headers.set("Accept-Language", acceptLanguage);
        }

        return headers;
    }

    protected <T> ResponseEntity<ApiResult<T>> requestPost(String url, String token, String acceptLanguage, Object body, ParameterizedTypeReference<ApiResult<T>> responseType) {
        HttpHeaders headers = createHeaders(token, acceptLanguage, null, false);

        return exchangeForAppResponse(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    protected <T> ResponseEntity<ApiResult<T>> requestPostDpop(String url,
                                                                 String token,
                                                                 String acceptLanguage,
                                                                 String dpopProof,
                                                                 Object body,
                                                                 ParameterizedTypeReference<ApiResult<T>> responseType) {
        HttpHeaders headers = createHeaders(token, acceptLanguage, dpopProof, true);

        return exchangeForAppResponse(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    protected ResponseEntity<ApiResult<Object>> requestPost(String url, String token, String acceptLanguage, Record body) {
        return requestPost(url, token, acceptLanguage, body, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResult<Object>> requestPost(String url, String acceptLanguage, Record body) {
        return requestPost(url, null, acceptLanguage, body);
    }

    protected <T> ResponseEntity<ApiResult<T>> requestGet(String url, String token, ParameterizedTypeReference<ApiResult<T>> responseType) {
        HttpHeaders headers = createHeaders(token, null, null, false);

        return exchangeForAppResponse(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    protected <T> ResponseEntity<ApiResult<T>> requestGetDpop(String url, String token, String dpopProof, ParameterizedTypeReference<ApiResult<T>> responseType) {
        HttpHeaders headers = createHeaders(token, null, dpopProof, true);

        return exchangeForAppResponse(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    protected ResponseEntity<ApiResult<Object>> requestGet(String url, String token) {
        return requestGet(url, token, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResult<Object>> requestGet(String url) {
        return requestGet(url, null);
    }

    protected ResponseEntity<ApiResult<TokenResponse>> loginRequest(String username,
                                                                      String password,
                                                                      String clientId,
                                                                      String clientSecret) {
        return loginRequest(username, password, clientId, clientSecret, null);
    }

    protected ResponseEntity<ApiResult<TokenResponse>> loginRequest(String username,
                                                                       String password,
                                                                       String clientId,
                                                                       String clientSecret,
                                                                       String dpopProof) {
        SignInRequest request = new SignInRequest(username, password, clientId, clientSecret);
        return requestPostDpop(loginUrl, null, null, dpopProof, request, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResult<TokenResponse>> loginRequest(String username, String password) {
        return loginRequest(username, password, props.getClientId(), props.getClientSecret());
    }

    protected TokenResponse loginAndGetData(String username, String password) {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(username, password);
        return assertStatusAndBodyAndReturnBody(response, TokenResponse.class);
    }

    protected String loginAndGetAccess(String username, String password) {
        return loginAndGetData(username, password).accessToken();
    }

    protected String loginAndGetRefresh(String username, String password) {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest(username, password);
        return extractRefreshCookie(response.getHeaders());
    }

    private String extractRefreshCookie(HttpHeaders headers) {
        return headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.split(";")[0].substring("refresh_token=".length()))
                .findFirst()
                .orElse(null);
    }

    protected ResponseEntity<ApiResult<Void>> logoutRequest(String refreshToken,
                                                               String clientId,
                                                               String clientSecret) {
        return logoutRequest(refreshToken, clientId, clientSecret, null);
    }

    protected ResponseEntity<ApiResult<Void>> logoutRequest(String refreshToken,
                                                               String clientId,
                                                               String clientSecret,
                                                               String dpopProof) {
        SignOutRequest req = new SignOutRequest(clientId, clientSecret);
        return requestPostWithCookie(logoutUrl, refreshToken, dpopProof, req, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResult<Void>> logoutRequest(String refreshToken) {
        return logoutRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    protected ResponseEntity<ApiResult<TokenResponse>> refreshRequest(String refreshToken,
                                                                         String clientId,
                                                                         String clientSecret) {
        return refreshRequest(refreshToken, clientId, clientSecret, null);
    }

    protected ResponseEntity<ApiResult<TokenResponse>> refreshRequest(String refreshToken,
                                                                          String clientId,
                                                                          String clientSecret,
                                                                          String dpopProof) {
        RefreshRequest request = new RefreshRequest(clientId, clientSecret);
        return requestPostWithCookie(refreshUrl, refreshToken, dpopProof, request, new ParameterizedTypeReference<>() {});
    }

    protected ResponseEntity<ApiResult<TokenResponse>> refreshRequest(String refreshToken) {
        return refreshRequest(refreshToken, props.getClientId(), props.getClientSecret());
    }

    private <T> ResponseEntity<ApiResult<T>> requestPostWithCookie(String url,
                                                                       String refreshToken,
                                                                       String dpopProof,
                                                                       Object body,
                                                                       ParameterizedTypeReference<ApiResult<T>> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        if (refreshToken != null && !refreshToken.isBlank()) {
            headers.set(HttpHeaders.COOKIE, "refresh_token=" + refreshToken);
        }
        if (dpopProof != null && !dpopProof.isBlank()) {
            headers.set("DPoP", dpopProof);
        }
        return exchangeForAppResponse(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    // Helper: do POST and return typed data (convert via ObjectMapper)
    protected <T> T postAndReturnData(String url, String token, Object body, Class<T> clazz) {
        ResponseEntity<ApiResult<T>> resp = requestPost(url, token, null, body, new ParameterizedTypeReference<>() {});
        return assertStatusAndBodyAndReturnBody(resp, clazz);
    }

    protected <T> T postAndReturnData(String url, String token, Object body, HttpStatus expectedStatus, Class<T> clazz) {
        ResponseEntity<ApiResult<T>> resp = requestPost(url, token, null, body, new ParameterizedTypeReference<>() {});
        return assertStatusAndBodyAndReturnBody(resp, expectedStatus, clazz);
    }

    // Helper: do GET and return typed data (convert via ObjectMapper)
    protected <T> T getAndReturnData(String url, String token, Class<T> clazz) {
        ResponseEntity<ApiResult<T>> resp = requestGet(url, token, new ParameterizedTypeReference<>() {});
        return assertStatusAndBodyAndReturnBody(resp, clazz);
    }

    private <T> ResponseEntity<ApiResult<T>> exchangeForAppResponse(String url,
                                                                      HttpMethod method,
                                                                      HttpEntity<?> entity,
                                                                       ParameterizedTypeReference<ApiResult<T>> responseType) {
        ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
        ApiResult<T> body = deserializeAppResponse(response.getBody(), responseType);

        if (body == null) {
            body = fallbackErrorBody(response.getStatusCode());
        }

        return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
    }

    private <T> ApiResult<T> deserializeAppResponse(String rawBody,
                                                      ParameterizedTypeReference<ApiResult<T>> responseType) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }

        try {
            if (responseType != null) {
                return objectMapper.readValue(
                        rawBody,
                        objectMapper.getTypeFactory().constructType(responseType.getType())
                );
            }

            return objectMapper.readValue(rawBody, new TypeReference<ApiResult<T>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize API response: " + rawBody, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResult<T> fallbackErrorBody(HttpStatusCode statusCode) {
        if (statusCode != null && statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            return (ApiResult<T>) ApiResult.error(
                    ApiErrorType.UNAUTHORIZED,
                    ApiErrorType.UNAUTHORIZED.message()
            );
        }

        if (statusCode != null && statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            return (ApiResult<T>) ApiResult.error(
                    ApiErrorType.FORBIDDEN,
                    ApiErrorType.FORBIDDEN.message()
            );
        }

        return null;
    }
}
