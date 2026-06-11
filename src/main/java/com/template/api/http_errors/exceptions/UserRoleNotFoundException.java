package com.template.api.http_errors.exceptions;

public class UserRoleNotFoundException extends RuntimeException {

    public UserRoleNotFoundException(String subject) {
        super("No role assignment found for subject '" + subject + "'");
    }
}
