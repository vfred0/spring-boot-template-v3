package com.template.api.http_errors.exceptions;

public class RoleAlreadyExistsException extends RuntimeException {
    public RoleAlreadyExistsException(String name) {
        super("Role '" + name + "' already exists");
    }
}
