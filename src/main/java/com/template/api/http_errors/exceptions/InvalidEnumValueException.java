package com.template.api.http_errors.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class InvalidEnumValueException extends RuntimeException {
    private final String rejectedValue;
    private final String fieldName;
    private final String code  = "VALIDATION_ERROR_ENUM_VALUE";

    public InvalidEnumValueException(String rejectedValue, String fieldName, String allowedValues) {
        super("Invalid enum value for field '" + fieldName + "': [" + rejectedValue + "]. Allowed values: " + allowedValues);
        this.rejectedValue = rejectedValue;
        this.fieldName = fieldName;
    }
}