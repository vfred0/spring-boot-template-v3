package com.template.api.http_errors.exceptions;

public class UnknownPermissionException extends RuntimeException {
    public UnknownPermissionException(String resource, String action) {
        super("Unknown permission '" + resource + ":" + action + "'. Permissions must exist in the catalog before assignment.");
    }
}
