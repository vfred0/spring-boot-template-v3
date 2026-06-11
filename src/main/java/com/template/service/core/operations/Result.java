package com.template.service.core.operations;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    static <T, E> Success<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Failure<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default T getOrNull() {
        if (this instanceof Success<T, E>(T value)) {
            return value;
        }
        return null;
    }

    default E getErrorOrNull() {
        if (this instanceof Failure<T, E>(E error)) {
            return error;
        }
        return null;
    }

    default <R> R fold(Function<T, R> onSuccess, Function<E, R> onFailure) {
        return switch (this) {
            case Success<T, E>(var value) -> onSuccess.apply(value);
            case Failure<T, E>(var error) -> onFailure.apply(error);
        };
    }

    static <T, E> Result<T, E> runCatching(Supplier<T> supplier, Function<Exception, E> onError) {
        try {
            return Result.success(supplier.get());
        } catch (Exception e) {
            return Result.failure(onError.apply(e));
        }
    }

    default <R> Result<R, E> map(Function<T, R> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> Result.success(mapper.apply(value));
            case Failure<T, E>(var error) -> Result.failure(error);
        };
    }

    default <R> Result<R, E> flatMap(Function<T, Result<R, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> Result.failure(error);
        };
    }

}
