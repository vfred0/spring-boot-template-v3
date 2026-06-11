package com.template.service.core.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NestedPatchResolver {

    private final ModelMapper mapper;

    public NestedPatchResolver() {
        this(new ModelMapper());
        this.mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
    }


    public <E, D> void resolve(D dto, E entity, NestedPatchDef<?, ?>[] defs) {
        for (NestedPatchDef<?, ?> def : defs) {
            applyNestedPatch(dto, entity, def);
        }
    }



    private <E, D, NESTED_DTO, NESTED_ENTITY> void applyNestedPatch(
            D dto,
            E entity,
            NestedPatchDef<NESTED_DTO, NESTED_ENTITY> def
    ) {
        Object dtoFieldValue = FieldAccessor.get(dto, def.fieldName());
        if (dtoFieldValue == null) return;

        if (dtoFieldValue instanceof Collection<?> collection) {
            applyCollectionPatch(
                    entity, def, castCollection(collection)
            );
        } else {
            applyObjectPatch(
                    entity, def, castUnchecked(dtoFieldValue)
            );
        }

        FieldAccessor.set(dto, def.fieldName(), null);
    }

    private <E, NESTED_DTO, NESTED_ENTITY> void applyCollectionPatch(
            E entity,
            NestedPatchDef<NESTED_DTO, NESTED_ENTITY> def,
            Collection<NESTED_DTO> dtoCollection
    ) {
        if (dtoCollection.isEmpty()) return;

        Collection<NESTED_ENTITY> entityCollection = FieldAccessor.get(entity, def.fieldName());
        if (entityCollection == null) return;

        Map<Object, NESTED_ENTITY> entityById = entityCollection.stream()
                .collect(Collectors.toMap(
                        e -> FieldAccessor.get(e, def.idField()),
                        Function.identity()
                ));

        for (NESTED_DTO dtoItem : dtoCollection) {
            Object dtoId = FieldAccessor.get(dtoItem, def.idField());
            if (dtoId == null) continue;

            NESTED_ENTITY matched = entityById.get(dtoId);
            if (matched != null) {
                mapper.map(dtoItem, matched);
            } else {
                entityCollection.add(mapper.map(dtoItem, def.entityClass()));
            }
        }
    }

    private <E, NESTED_DTO, NESTED_ENTITY> void applyObjectPatch(
            E entity,
            NestedPatchDef<NESTED_DTO, NESTED_ENTITY> def,
            NESTED_DTO dtoValue
    ) {
        NESTED_ENTITY existing = FieldAccessor.get(entity, def.fieldName());
        if (existing != null) {
            mapper.map(dtoValue, existing);
        } else {
            FieldAccessor.set(entity, def.fieldName(), mapper.map(dtoValue, def.entityClass()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> castCollection(Collection<?> raw) {
        return (Collection<T>) raw;
    }

    @SuppressWarnings("unchecked")
    private static <T> T castUnchecked(Object obj) {
        return (T) obj;
    }
}