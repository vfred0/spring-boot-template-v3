package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.template.api.dtos.api_key.ApiKeyResponse;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.rbac.AssignPermissionsRequest;
import com.template.api.dtos.rbac.AssignRoleRequest;
import com.template.api.dtos.rbac.CreateRoleRequest;
import com.template.api.dtos.rbac.PermissionRef;
import com.template.api.dtos.rbac.RoleResponse;
import com.template.service.core.rbac.RbacManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('rbac:manage')")
@Tag(name = "RBAC Admin", description = "Manage roles, permissions and role assignments")
public class RbacAdminResource {

    private final RbacManagementService managementService;

    @PostMapping("/roles")
    @Operation(summary = "Create role", description = "Creates a role. Permissions are attached separately.")
    public ResponseEntity<ApiResult<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.created(managementService.createRole(request)));
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles", description = "Lists all roles with their attached permissions.")
    public ApiResult<List<RoleResponse>> listRoles() {
        return ApiResult.ok(managementService.listRoles());
    }

    @GetMapping("/permissions")
    @Operation(summary = "List permissions", description = "Lists the permission catalog available to attach to roles.")
    public ApiResult<List<PermissionRef>> listPermissions() {
        return ApiResult.ok(managementService.listPermissions());
    }

    @PostMapping("/permissions")
    @Operation(summary = "Create permission", description = "Adds a new permission to the catalog. Returns 409 if it already exists.")
    public ResponseEntity<ApiResult<PermissionRef>> createPermission(@Valid @RequestBody PermissionRef request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.created(managementService.createPermission(request)));
    }

    @PostMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Attach permissions", description = "Adds catalog permissions to a role (additive, keeps existing).")
    public ApiResult<RoleResponse> addPermissions(@PathVariable("roleId") Long roleId,
                                                  @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResult.ok(managementService.addPermissions(roleId, request));
    }

    @PatchMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Replace permissions", description = "Replaces the role's entire permission set with the given list.")
    public ApiResult<RoleResponse> replacePermissions(@PathVariable("roleId") Long roleId,
                                                      @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResult.ok(managementService.replacePermissions(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}/permissions/{resource}/{action}")
    @Operation(summary = "Detach permission", description = "Removes a single permission from a role. The catalog entry is kept.")
    public ApiResult<RoleResponse> removePermission(@PathVariable("roleId") Long roleId,
                                                    @PathVariable("resource") String resource,
                                                    @PathVariable("action") String action) {
        return ApiResult.ok(managementService.removePermission(roleId, resource, action));
    }

    @PutMapping("/users/{subject}/role")
    @Operation(summary = "Assign role", description = "Assigns a role to a subject, replacing any previous role.")
    public ApiResult<RoleResponse> assignRole(@PathVariable("subject") String subject,
                                              @Valid @RequestBody AssignRoleRequest request) {
        return ApiResult.ok(managementService.assignRole(subject, request));
    }

    @DeleteMapping("/users/{subject}/role")
    @Operation(summary = "Unassign role", description = "Removes a subject's role assignment. Returns 404 if none exists.")
    public ResponseEntity<Void> unassignRole(@PathVariable("subject") String subject) {
        managementService.unassignRole(subject);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "Delete role", description = "Deletes a role. Returns 409 if any subjects are still assigned to it.")
    public ResponseEntity<Void> deleteRole(@PathVariable("roleId") Long roleId) {
        managementService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{subject}/keys")
    @Operation(summary = "List subject API keys", description = "Lists API key metadata for a given subject.")
    public ApiResult<List<ApiKeyResponse>> listKeys(@PathVariable("subject") String subject) {
        return ApiResult.ok(managementService.listKeysForSubject(subject));
    }
}
