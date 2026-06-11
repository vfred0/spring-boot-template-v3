package com.template.service.core.operations.bulk;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiFieldError;
import com.template.api.http_errors.exceptions.BulkItemProcessingException;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import com.template.service.core.operations.Result;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import com.template.service.core.operations.validation.AbstractValidatingService;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public abstract class BulkService<CREATE, PATCH> extends AbstractValidatingService {

    protected BulkService(Validator validator, ValidationRouteRegistry routeRegistry) {
        super(validator, routeRegistry);
    }

    public CompletableFuture<Result<List<CREATE>, ApiResult<Void>>> create(Set<CREATE> items) {
        return processBatch(items, "create", this::onValidCreate);
    }

    public CompletableFuture<Result<List<PATCH>, ApiResult<Void>>> patch(Set<PATCH> items) {
        return processBatch(items, "patch", this::onValidPatch);
    }

    protected abstract void onValidCreate(CREATE item);

    protected abstract void onValidPatch(PATCH item);

    private <T> CompletableFuture<Result<List<T>, ApiResult<Void>>> processBatch(
            Set<T> items, String operation, Consumer<T> processor) {
        var itemList = List.copyOf(items);
        var validationResults = validateAll(itemList);
        var validItems = successItems(validationResults);
        var processingErrors = processValid(itemList, validationResults, processor);
        var allErrors = collectAllErrors(validationResults, processingErrors);
        log.info("Batch {} on [{}] — total: {}, valid: {}, failed: {}",
                operation, resolveRouteInfo(), itemList.size(), validItems.size(), allErrors.size());
        if (allErrors.isEmpty()) return CompletableFuture.completedFuture(Result.success(validItems));
        return CompletableFuture.completedFuture(
                Result.failure(buildFailureResponse(validItems, itemList.size(), processingErrors, allErrors)));
    }

    private <T> List<T> successItems(List<Result<T, List<ApiFieldError>>> results) {
        return results.stream().filter(Result::isSuccess).map(Result::getOrNull).toList();
    }

    private ApiResult<Void> buildFailureResponse(List<?> validItems, int total,
                                                    List<ApiFieldError> processingErrors, List<ApiFieldError> allErrors) {
        int processed = validItems.size() - processingErrors.size();
        int failed = (total - validItems.size()) + processingErrors.size();
        return ApiResult.bulk(allErrors, processed, failed);
    }

    private <T> List<Result<T, List<ApiFieldError>>> validateAll(List<T> items) {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAll())) {
            var subtasks = IntStream.range(0, items.size())
                    .mapToObj(i -> scope.fork(() -> validateItem(items.get(i), i)))
                    .toList();
            scope.join();
            return subtasks.stream().map(StructuredTaskScope.Subtask::get).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        }
    }

    private <T> List<ApiFieldError> processValid(List<T> items,
                                                  List<Result<T, List<ApiFieldError>>> validationResults,
                                                  Consumer<T> processor) {
        var validIndices = IntStream.range(0, items.size())
                .filter(i -> validationResults.get(i).isSuccess())
                .boxed().toList();
        if (validIndices.isEmpty()) return List.of();
        try (var scope = StructuredTaskScope.open()) {
            var subtasks = validIndices.stream()
                    .map(i -> scope.fork(() -> tryExecuteItem(items.get(i), processor)))
                    .toList();
            scope.join();
            return subtasks.stream().map(StructuredTaskScope.Subtask::get).flatMap(Optional::stream).toList();
        } catch (StructuredTaskScope.FailedException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        }
    }

    private <T> Optional<ApiFieldError> tryExecuteItem(T item, Consumer<T> processor) {
        try {
            processor.accept(item);
            return Optional.empty();
        } catch (BulkItemProcessingException ex) {
            var errors = new ArrayList<ApiFieldError>();
            handleProcessingException(ex, errors);
            return errors.stream().findFirst();
        } catch (RuntimeException ex) {
            throw new InternalServerErrorException();
        }
    }

    private <T> List<ApiFieldError> collectAllErrors(List<Result<T, List<ApiFieldError>>> results,
                                                      List<ApiFieldError> processingErrors) {
        return Stream.concat(
                results.stream().filter(Result::isFailure).flatMap(r -> r.getErrorOrNull().stream()),
                processingErrors.stream()
        ).toList();
    }
}
