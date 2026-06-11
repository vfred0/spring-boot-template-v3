package com.template.api.http_errors.exceptions;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(Object roleReference) {
        super("Role '" + roleReference + "' not found");
    }
}
