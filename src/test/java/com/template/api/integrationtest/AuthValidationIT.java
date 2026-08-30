package com.template.api.integrationtest;

import com.template.MainApplication;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.util.AbstractIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import com.template.config.security.RateLimitingFilter;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainApplication.class)
class AuthValidationIT extends AbstractIntegrationTest {

    AuthValidationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                     CacheManager cacheManager,
                     RateLimitingFilter rateLimitingFilter) {
        super(props, cacheManager, rateLimitingFilter);
    }

    @Test
    void login_emptyFields_badRequest() {
        ResponseEntity<ApiResult<TokenResponse>> response = loginRequest("", "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResult<TokenResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ApiErrorType.BAD_REQUEST.code());
        assertThat(body.message()).isEqualTo(ApiErrorType.BAD_REQUEST.message());

        Set<String> fieldErrors = body.errors().stream()
                .map(e -> e.property() + ": " + e.message())
                .collect(Collectors.toSet());
        assertThat(fieldErrors).isEqualTo(Set.of(
                "username: El nombre de usuario es requerido",
                "password: La contraseña es requerida"
        ));
    }

    // RefreshRequest carries no client-submitted fields (it's the refresh_token cookie
    // that matters), so there's no body to fail @Valid on. The real validation path is
    // AuthResource.refresh()'s own guard for a missing/blank refresh_token cookie.
    @Test
    void refresh_missingCookie_unauthorized() {
        ResponseEntity<ApiResult<TokenResponse>> response = refreshRequest(null);

        assertErrorStatusAndBody(response, HttpStatus.UNAUTHORIZED,
                ApiErrorType.INVALID_GRANT.code(),
                "Tu sesión no pudo ser renovada. Inicia sesión nuevamente.");
    }
}
