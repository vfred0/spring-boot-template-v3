package com.template.api.http_errors.exceptions;

public class RoleInUseException extends RuntimeException {

    public RoleInUseException(Long roleId) {
        super("Role '" + roleId + "' is still assigned to users and cannot be deleted");
    }
}
