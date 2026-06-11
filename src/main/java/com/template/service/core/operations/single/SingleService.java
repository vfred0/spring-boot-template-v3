package com.template.service.core.operations.single;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.ApiFieldError;
import com.template.api.http_errors.exceptions.BulkItemProcessingException;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import com.template.service.core.operations.Result;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import com.template.service.core.operations.validation.AbstractValidatingService;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
public abstract class SingleService<CREATE, PATCH> extends AbstractValidatingService {

    protected SingleService(Validator validator, ValidationRouteRegistry routeRegistry) {
        super(validator, routeRegistry);
    }

    public Result<CREATE, ApiResult<Void>> create(CREATE item) {
        return processSingle(item, "create", this::onValidCreate);
    }

    public Result<PATCH, ApiResult<Void>> patch(PATCH item) {
        return processSingle(item, "patch", this::onValidPatch);
    }

    protected abstract void onValidCreate(CREATE item);

    protected abstract void onValidPatch(PATCH item);

    private <T> Result<T, ApiResult<Void>> processSingle(T item, String operation, Consumer<T> processor) {
        var routeInfo = resolveRouteInfo();
        var validationResult = validateItem(item, -1);
        var validationErrors = validationResult.isFailure() ? validationResult.getErrorOrNull() : List.<ApiFieldError>of();
        var processingErrors = new ArrayList<ApiFieldError>();
        var processingErrorType = validationResult.isSuccess() ? executeItem(item, processor, processingErrors) : ApiErrorType.BAD_REQUEST;
        var allErrors = Stream.concat(validationErrors.stream(), processingErrors.stream()).toList();

        if (!allErrors.isEmpty()) {
            var type = !validationErrors.isEmpty() ? ApiErrorType.BAD_REQUEST : processingErrorType;
            log.info("Single {} on [{}] — failed: {} error(s)", operation, routeInfo, allErrors.size());
            return Result.failure(ApiResult.errors(type, allErrors));
        }

        log.info("Single {} on [{}] — success", operation, routeInfo);
        return Result.success(item);
    }

    private <T> ApiErrorType executeItem(T item, Consumer<T> processor, List<ApiFieldError> errors) {
        try {
            processor.accept(item);
            return ApiErrorType.BAD_REQUEST;
        } catch (BulkItemProcessingException ex) {
            handleProcessingException(ex, errors);
            return resolveErrorType(ex.getException());
        } catch (RuntimeException ex) {
            throw new InternalServerErrorException();
        }
    }

    private ApiErrorType resolveErrorType(RuntimeException ex) {
        String code = errorCode(ex);
        return Arrays.stream(ApiErrorType.values())
                .filter(t -> t.code().equals(code))
                .findFirst()
                .orElse(ApiErrorType.INTERNAL_SERVER_ERROR);
    }
}
