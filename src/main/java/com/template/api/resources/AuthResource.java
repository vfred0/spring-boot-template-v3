package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.auth.KeycloakTokenResponse;
import com.template.api.dtos.auth.SignInRequest;
import com.template.api.dtos.auth.SignOutRequest;
import com.template.api.dtos.auth.RefreshRequest;
import com.template.api.dtos.auth.TokenResponse;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.config.security.RefreshTokenCookieWriter;
import com.template.service.core.KeycloakAuthService;
import com.template.service.core.shared.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Keycloak authentication endpoints")
public class AuthResource {

    private final KeycloakAuthService authService;
    private final RefreshTokenCookieWriter cookieWriter;
    private final MessageService messageService;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain tokens",
            description = "Authenticates user. Returns access token in body and sets refresh token as HttpOnly cookie.")
    @ApiResponse(responseCode = "200", description = "Tokens issued",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<ApiResult<TokenResponse>> login(
            @Valid @RequestBody SignInRequest req,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        try {
            KeycloakTokenResponse tokens = authService.login(req, dpopProof);
            cookieWriter.write(response, tokens.getRefreshToken(), (int) tokens.getRefreshExpiresIn());
            return ResponseEntity.ok(ApiResult.ok(TokenResponse.from(tokens)));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(ApiErrorType.UNAUTHORIZED,
                            messageService.getMessage("auth.error.login.title"),
                            messageService.getMessage("auth.error.login.message")));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens",
            description = "Exchanges the refresh token cookie for a new access token. Rotates the refresh token cookie.")
    @ApiResponse(responseCode = "200", description = "Tokens refreshed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Refresh token missing or expired",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<ApiResult<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest req,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(ApiErrorType.INVALID_GRANT,
                            messageService.getMessage("auth.error.refresh.title"),
                            messageService.getMessage("auth.error.refresh.message")));
        }
        try {
            KeycloakTokenResponse tokens = authService.refresh(req, refreshToken, dpopProof);
            cookieWriter.write(response, tokens.getRefreshToken(), (int) tokens.getRefreshExpiresIn());
            return ResponseEntity.ok(ApiResult.ok(TokenResponse.from(tokens)));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(ApiErrorType.INVALID_GRANT,
                            messageService.getMessage("auth.error.refresh.title"),
                            messageService.getMessage("auth.error.refresh.message")));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token in Keycloak and clears the refresh token cookie.")
    @ApiResponse(responseCode = "200", description = "Logged out",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<ApiResult<Void>> logout(
            @Valid @RequestBody SignOutRequest req,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        try {
            if (StringUtils.hasText(refreshToken)) {
                authService.logout(req, refreshToken, dpopProof);
            }
            cookieWriter.clear(response);
            return ResponseEntity.ok(ApiResult.<Void>ok(null));
        } catch (KeycloakAuthException ex) {
            cookieWriter.clear(response);
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(ApiErrorType.INVALID_TOKEN,
                            messageService.getMessage("auth.error.logout.title"),
                            messageService.getMessage("auth.error.logout.message")));
        }
    }
}
