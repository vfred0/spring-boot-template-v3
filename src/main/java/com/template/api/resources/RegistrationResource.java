package com.template.api.resources;

import com.template.api.dtos.auth.RegisterRequest;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.service.core.KeycloakUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Keycloak authentication endpoints")
public class RegistrationResource {

    private final KeycloakUserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Creates the user in Keycloak. The password is hashed by Keycloak (argon2 by default since v24).")
    @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "409", description = "Username already taken",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<ApiResult<Void>> register(@Valid @RequestBody RegisterRequest req) {
        try {
            userService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(null));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(errorType(ex), ex.getKeycloakMessage()));
        }
    }

    private ApiErrorType errorType(KeycloakAuthException ex) {
        return ex.getStatus() == HttpStatus.CONFLICT
                ? ApiErrorType.CONFLICT
                : ApiErrorType.SERVICE_UNAVAILABLE;
    }
}
