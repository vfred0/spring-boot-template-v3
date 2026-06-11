package com.template.service.core.mapper;

import com.template.api.http_errors.exceptions.InternalServerErrorException;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FieldAccessor {

    private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private FieldAccessor() {}

    @SuppressWarnings("unchecked")
    public static <T> T get(Object target, String fieldName) {
        try {
            Field field = resolve(target.getClass(), fieldName);
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new InternalServerErrorException("Error accessing field: " + fieldName);
        }
    }

    public static void set(Object target, String fieldName, Object value) {
        try {
            Field field = resolve(target.getClass(), fieldName);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new InternalServerErrorException("Error setting field: " + fieldName);
        }
    }

    private static Field resolve(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#" + fieldName;
        return FIELD_CACHE.computeIfAbsent(cacheKey, _ -> findField(clazz, fieldName));
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new InternalServerErrorException("Field not found: " + fieldName);
    }
}