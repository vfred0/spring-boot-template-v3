package com.template.service.core.mapper;


import lombok.RequiredArgsConstructor;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class Mapper {

    private final ModelMapper mapper;
    private final NestedPatchResolver nestedPatchResolver;

    public <E, D> E mapToCreate(D dto, E entity) {
        validateInputs(dto, entity);
        mapper.map(dto, entity);
        return entity;
    }

    public <E, D> E mapToPatch(D dto, E entity, String checkField, NestedPatchDef<?, ?>... nestedPatchDefs) {
        validateInputs(dto, entity);

        Object dtoId = FieldAccessor.get(dto, checkField);
        Object entityId = FieldAccessor.get(entity, checkField);

        if (!Objects.equals(dtoId, entityId)) return entity;

        nestedPatchResolver.resolve(dto, entity, nestedPatchDefs);
        mapper.map(dto, entity);
        return entity;
    }

    private void validateInputs(Object dto, Object entity) {
        if (dto == null || entity == null) {
            throw new InternalServerErrorException("DTO and entity cannot be null");
        }
    }
}