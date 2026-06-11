package com.template.api.dtos.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.template.api.http_errors.exceptions.InvalidEnumValueException;

@Getter
@Slf4j
public enum CustomerStatus {
    NEW("NUEVO"),
    CONFIRMED("CONFIRMADO"),
    IN_PROGRESS("EN PROGRESO");

    private final String value;

    CustomerStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CustomerStatus fromValue(String value) {
        for (CustomerStatus type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) return type;
        }
        throw new InvalidEnumValueException(
                value,
                "status",
                "'NUEVO', 'CONFIRMADO', 'EN PROGRESO'"
        );
    }

    @JsonValue
    public String getValue() {
        return value;
    }


    public String getKey() {
        return this.name();
    }
}
