package com.template.api.http_errors.exceptions;

public class PermissionAlreadyExistsException extends RuntimeException {

    public PermissionAlreadyExistsException(String resource, String action) {
        super("Permission '" + resource + ":" + action + "' already exists");
    }
}
