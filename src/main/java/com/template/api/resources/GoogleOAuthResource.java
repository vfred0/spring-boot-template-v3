package com.template.api.resources;

import com.template.api.dtos.auth.AuthorizationUrlResponse;
import com.template.api.dtos.auth.KeycloakTokenResponse;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.config.keycloak.KeycloakProperties;
import com.template.config.security.OAuthStateManager;
import com.template.config.security.RefreshTokenCookieWriter;
import com.template.service.core.GoogleOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Google OAuth2 endpoints")
public class GoogleOAuthResource {

    private final GoogleOAuthService oAuthService;
    private final OAuthStateManager stateManager;
    private final RefreshTokenCookieWriter cookieWriter;
    private final KeycloakProperties props;

    @GetMapping("/google/initiate")
    @Operation(summary = "Initiate Google login",
            description = "Returns the Keycloak authorization URL. Frontend must redirect the user to it.")
    public ResponseEntity<ApiResult<AuthorizationUrlResponse>> initiateGoogleLogin(HttpServletResponse response) {
        String state = stateManager.generateAndWrite(response);
        String authorizationUrl = oAuthService.buildAuthorizationUrl(state);
        return ResponseEntity.ok(ApiResult.ok(new AuthorizationUrlResponse(authorizationUrl)));
    }

    @GetMapping("/callback")
    @Operation(summary = "OAuth2 callback",
            description = "Receives the authorization code from Keycloak, exchanges it for tokens, and redirects to the frontend.")
    public void handleOAuthCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String cookieState = stateManager.extract(request);
        stateManager.clear(response);

        if (!isStateValid(state, cookieState)) {
            log.warn("OAuth2 state mismatch — possible CSRF attempt");
            response.sendRedirect(buildFrontendUrl("error", ApiErrorType.UNAUTHORIZED.code()));
            return;
        }

        try {
            KeycloakTokenResponse tokens = oAuthService.exchangeCode(code);
            cookieWriter.write(response, tokens.getRefreshToken(), (int) tokens.getRefreshExpiresIn());
            response.sendRedirect(buildFrontendSuccessUrl(tokens));
        } catch (KeycloakAuthException ex) {
            log.error("OAuth2 exchange failed: {}", ex.getMessage());
            response.sendRedirect(buildFrontendUrl("error", "auth_failed"));
        }
    }

    private boolean isStateValid(String state, String cookieState) {
        return StringUtils.hasText(cookieState) && cookieState.equals(state);
    }

    private String buildFrontendSuccessUrl(KeycloakTokenResponse tokens) {
        return UriComponentsBuilder.fromUriString(props.getOauth2FrontendCallbackUrl())
                .queryParam("access_token", tokens.getAccessToken())
                .queryParam("expires_in", tokens.getExpiresIn())
                .toUriString();
    }

    private String buildFrontendUrl(String key, String value) {
        return UriComponentsBuilder.fromUriString(props.getOauth2FrontendCallbackUrl())
                .queryParam(key, value)
                .toUriString();
    }
}
