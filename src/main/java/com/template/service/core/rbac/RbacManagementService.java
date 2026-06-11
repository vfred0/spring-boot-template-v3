package com.template.service.core.rbac;

import lombok.RequiredArgsConstructor;
import com.template.api.dtos.api_key.ApiKeyResponse;
import com.template.api.dtos.rbac.AssignPermissionsRequest;
import com.template.api.dtos.rbac.AssignRoleRequest;
import com.template.api.dtos.rbac.CreateRoleRequest;
import com.template.api.dtos.rbac.PermissionRef;
import com.template.api.dtos.rbac.RoleResponse;
import com.template.api.http_errors.exceptions.PermissionAlreadyExistsException;
import com.template.api.http_errors.exceptions.RoleAlreadyExistsException;
import com.template.api.http_errors.exceptions.RoleInUseException;
import com.template.api.http_errors.exceptions.RoleNotFoundException;
import com.template.api.http_errors.exceptions.UnknownPermissionException;
import com.template.api.http_errors.exceptions.UserRoleNotFoundException;
import com.template.data.daos.ApiKeyRepository;
import com.template.data.daos.PermissionRepository;
import com.template.data.daos.RoleRepository;
import com.template.data.daos.UserRoleRepository;
import com.template.data.entities.core.rbac.Permission;
import com.template.data.entities.core.rbac.Role;
import com.template.data.entities.core.rbac.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RbacManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        roleRepository.findByName(request.name()).ifPresent(existing -> {
            throw new RoleAlreadyExistsException(request.name());
        });
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse addPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        request.permissions().forEach(ref -> role.getPermissions().add(resolve(ref)));
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse replacePermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        role.getPermissions().clear();
        request.permissions().forEach(ref -> role.getPermissions().add(resolve(ref)));
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse removePermission(Long roleId, String resource, String action) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        role.getPermissions().remove(resolve(new PermissionRef(resource, action)));
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse assignRole(String subject, AssignRoleRequest request) {
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new RoleNotFoundException(request.role()));
        UserRole userRole = userRoleRepository.findByKeycloakSub(subject).orElseGet(UserRole::new);
        userRole.setKeycloakSub(subject);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
        return RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        if (userRoleRepository.existsByRoleId(roleId)) {
            throw new RoleInUseException(roleId);
        }
        roleRepository.delete(role);
    }

    @Transactional
    public void unassignRole(String subject) {
        if (userRoleRepository.deleteByKeycloakSub(subject) == 0) {
            throw new UserRoleNotFoundException(subject);
        }
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(RoleResponse::from)
                .toList();
    }

    public List<PermissionRef> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> new PermissionRef(permission.getResource(), permission.getAction()))
                .toList();
    }

    @Transactional
    public PermissionRef createPermission(PermissionRef ref) {
        permissionRepository.findByResourceAndAction(ref.resource(), ref.action()).ifPresent(existing -> {
            throw new PermissionAlreadyExistsException(ref.resource(), ref.action());
        });
        Permission permission = new Permission();
        permission.setResource(ref.resource());
        permission.setAction(ref.action());
        Permission saved = permissionRepository.save(permission);
        return new PermissionRef(saved.getResource(), saved.getAction());
    }

    public List<ApiKeyResponse> listKeysForSubject(String subject) {
        return apiKeyRepository.findBySubject(subject).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    private Permission resolve(PermissionRef ref) {
        return permissionRepository.findByResourceAndAction(ref.resource(), ref.action())
                .orElseThrow(() -> new UnknownPermissionException(ref.resource(), ref.action()));
    }
}
