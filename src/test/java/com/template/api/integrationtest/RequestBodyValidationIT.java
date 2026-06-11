package com.template.api.integrationtest;

import com.template.MainApplication;
import com.template.api.util.AbstractIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.config.security.RateLimitingFilter;
import com.template.service.core.api_key.ApiKeyManagementService;
import com.template.service.core.api_key.CreateApiKeyCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = "app.security.mode=API_KEY")
class RequestBodyValidationIT extends AbstractIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyManagementService apiKeyManagementService;

    private String apiKey;
    private String customersUrl;

    RequestBodyValidationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                            CacheManager cacheManager,
                            RateLimitingFilter rateLimitingFilter,
                            ApiKeyManagementService apiKeyManagementService) {
        super(props, cacheManager, rateLimitingFilter);
        this.apiKeyManagementService = apiKeyManagementService;
    }

    @BeforeEach
    void issueKey() {
        apiKey = apiKeyManagementService.issue(
                new CreateApiKeyCommand("request-body-tester", "test key", null, null)).rawKey();
        customersUrl = mainUrl + "/customers";
    }

    @Test
    void authenticatedRequestWithMalformedJsonReturnsParseError() {
        String malformedJson = "{ \"id\": x\"xx-0\", \"names\": \"Jane\" }";

        ResponseEntity<String> response = postRaw(malformedJson);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("\"code\":\"BAD_REQUEST\"")
                .contains("JSON_PARSE_ERROR");
    }

    private ResponseEntity<String> postRaw(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
        headers.set(API_KEY_HEADER, apiKey);
        headers.set("X-API-VERSION", "2.0");
        return restTemplate.exchange(customersUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
