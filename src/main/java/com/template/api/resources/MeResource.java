package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.rbac.UserProfileResponse;
import com.template.service.core.rbac.UserPermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Current user profile")
public class MeResource {

    private final UserPermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's role and permissions.")
    public ResponseEntity<ApiResult<UserProfileResponse>> me(Authentication authentication) {
        String name = extractName(authentication);
        UserProfileResponse profile = permissionService.buildProfile(authentication.getName(), name);
        return ResponseEntity.ok(ApiResult.ok(profile));
    }

    private String extractName(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaim("given_name");
        }
        return null;
    }
}
