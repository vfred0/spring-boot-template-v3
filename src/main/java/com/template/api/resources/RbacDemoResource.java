package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.template.api.dtos.rbac.AccessProbeResponse;
import com.template.api.dtos.core.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "RBAC Demo", description = "Probes that confirm role and permission access regardless of auth mode")
public class RbacDemoResource {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Requires role ADMIN")
    public ApiResult<AccessProbeResponse> admin(Authentication authentication) {
        return probe(authentication, "ROLE_ADMIN");
    }

    @GetMapping("/customers/read")
    @PreAuthorize("hasAuthority('customers:read')")
    @Operation(summary = "Requires permission customers:read")
    public ApiResult<AccessProbeResponse> customersRead(Authentication authentication) {
        return probe(authentication, "customers:read");
    }

    @GetMapping("/customers/update")
    @PreAuthorize("hasAuthority('customers:update')")
    @Operation(summary = "Requires permission customers:update")
    public ApiResult<AccessProbeResponse> customersUpdate(Authentication authentication) {
        return probe(authentication, "customers:update");
    }

    @GetMapping("/customers/delete")
    @PreAuthorize("hasAuthority('customers:delete')")
    @Operation(summary = "Requires permission customers:delete")
    public ApiResult<AccessProbeResponse> customersDelete(Authentication authentication) {
        return probe(authentication, "customers:delete");
    }

    @GetMapping("/reports/read")
    @PreAuthorize("hasAuthority('reports:read')")
    @Operation(summary = "Requires permission reports:read")
    public ApiResult<AccessProbeResponse> reportsRead(Authentication authentication) {
        return probe(authentication, "reports:read");
    }

    @GetMapping("/reports/create")
    @PreAuthorize("hasAuthority('reports:create')")
    @Operation(summary = "Requires permission reports:create")
    public ApiResult<AccessProbeResponse> reportsCreate(Authentication authentication) {
        return probe(authentication, "reports:create");
    }

    private ApiResult<AccessProbeResponse> probe(Authentication authentication, String granted) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return ApiResult.ok(new AccessProbeResponse(authentication.getName(), granted, authorities));
    }
}
