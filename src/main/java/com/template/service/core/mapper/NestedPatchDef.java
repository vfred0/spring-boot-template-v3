package com.template.service.core.mapper;

import java.util.Objects;

public record NestedPatchDef<DTO, ENTITY>(
        String fieldName,
        String idField,
        Class<? extends ENTITY> entityClass
) {
    public NestedPatchDef {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(idField, "idField must not be null");
        Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    public static <DTO, ENTITY> NestedPatchDef<DTO, ENTITY> of(
            String fieldName,
            String idField,
            Class<? extends ENTITY> entityClass
    ) {
        return new NestedPatchDef<>(fieldName, idField, entityClass);
    }
}