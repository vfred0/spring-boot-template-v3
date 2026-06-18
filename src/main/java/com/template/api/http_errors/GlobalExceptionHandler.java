package com.template.api.http_errors;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.exceptions.AccountNotFoundException;
import com.template.api.http_errors.exceptions.AccountOptimisticLockException;
import com.template.api.http_errors.exceptions.ApiKeyNotFoundException;
import com.template.api.http_errors.exceptions.ClientNotFoundException;
import com.template.api.http_errors.exceptions.ClientSearchQueryTooShortException;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import com.template.api.http_errors.exceptions.PhoneAlreadyExistsException;
import com.template.api.http_errors.exceptions.RequestNotFoundException;
import com.template.api.http_errors.exceptions.PermissionAlreadyExistsException;
import com.template.api.http_errors.exceptions.RoleAlreadyExistsException;
import com.template.api.http_errors.exceptions.RoleInUseException;
import com.template.api.http_errors.exceptions.RoleNotFoundException;
import com.template.api.http_errors.exceptions.UnknownPermissionException;
import com.template.api.http_errors.exceptions.UserRoleNotFoundException;
import com.template.api.http_errors.request_body.RequestBodyErrorResolver;
import com.template.config.api_version.ApiVersion;
import com.template.service.core.operations.validation.ValidationMapper;
import com.template.service.core.shared.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TransactionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;
    private final RequestBodyErrorResolver requestBodyErrorResolver;

    // --- Request body / JSON parse (delegated) ---

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request,
                                                          HandlerMethod handlerMethod) {
        var response = requestBodyErrorResolver.resolve(ex, request, resolveBodyType(handlerMethod));
        return ResponseEntity.status(response.status()).contentType(MediaType.APPLICATION_JSON).body(response);
    }

    private Class<?> resolveBodyType(HandlerMethod handlerMethod) {
        if (handlerMethod == null) return null;
        for (var parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(RequestBody.class)) return parameter.getParameterType();
        }
        return null;
    }

    // --- Validation ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toApiFieldError)
                .toList();
        return ResponseEntity.status(ApiErrorType.BAD_REQUEST.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResult.errors(ApiErrorType.BAD_REQUEST, errors));
    }

    private ApiFieldError toApiFieldError(FieldError fieldError) {
        Object rejected = fieldError.getRejectedValue();
        return new ApiFieldError(
                rejected != null ? String.valueOf(rejected) : null,
                fieldError.getField(),
                fieldError.getField(),
                ValidationMapper.errorCode(fieldError.getCode()),
                fieldError.getDefaultMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(ApiErrorType.BAD_REQUEST,
                messageService.getMessage("error.typeMismatch", new Object[]{String.valueOf(ex.getValue())}));
    }

    // --- Domain ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex) {
        return respond(ApiErrorType.FORBIDDEN, messageService.getMessage("api.error.forbidden"));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleClientNotFound(ClientNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getApiKeyId()));
    }

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleRequestNotFound(RequestNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getRequestId()));
    }

    @ExceptionHandler(AccountOptimisticLockException.class)
    public ResponseEntity<ApiResult<Void>> handleOptimisticLock(AccountOptimisticLockException ex) {
        return respond(ApiErrorType.CONFLICT, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> handlePhoneExists(PhoneAlreadyExistsException ex) {
        return respond(ApiErrorType.CONFLICT, msg(ex.getMessageCode(), ex.getPhone()));
    }

    @ExceptionHandler(ClientSearchQueryTooShortException.class)
    public ResponseEntity<ApiResult<Void>> handleSearchQueryTooShort(ClientSearchQueryTooShortException ex) {
        return respond(ApiErrorType.BAD_REQUEST, msg(ex.getMessageCode(), ex.getMinLength()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleRoleNotFound(RoleNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserRoleNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleUserRoleNotFound(UserRoleNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> handleRoleAlreadyExists(RoleAlreadyExistsException ex) {
        return respond(ApiErrorType.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RoleInUseException.class)
    public ResponseEntity<ApiResult<Void>> handleRoleInUse(RoleInUseException ex) {
        return respond(ApiErrorType.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnknownPermissionException.class)
    public ResponseEntity<ApiResult<Void>> handleUnknownPermission(UnknownPermissionException ex) {
        return respond(ApiErrorType.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PermissionAlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> handlePermissionAlreadyExists(PermissionAlreadyExistsException ex) {
        return respond(ApiErrorType.CONFLICT, ex.getMessage());
    }

    // --- Transport / infrastructure ---

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ApiResult<Void>> handleInternalServerError(InternalServerErrorException ex) {
        return respond(ApiErrorType.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, TransactionException.class, ResourceAccessException.class})
    public ResponseEntity<ApiResult<Void>> handleServiceUnavailable(Exception ex) {
        return respond(ApiErrorType.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ResponseEntity<ApiResult<Void>> handleGatewayTimeout(Exception ex) {
        return respond(ApiErrorType.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<ApiResult<Void>> handleInvalidApiVersion(InvalidApiVersionException ex) {
        return respond(ApiErrorType.INVALID_API_VERSION,
                ApiErrorType.INVALID_API_VERSION.message() + ". Supported versions: " + ApiVersion.all());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return respond(ApiErrorType.METHOD_NOT_ALLOWED,
                "Not allowed method [" + ex.getMethod() + "] to " + request.getRequestURI());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NoHandlerFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return respond(ApiErrorType.INTERNAL_SERVER_ERROR, messageService.getMessage("api.error.internalServerError"));
    }

    private String msg(String code, Object arg) {
        return messageService.getMessage(code, new Object[]{String.valueOf(arg)});
    }

    private ResponseEntity<ApiResult<Void>> respond(ApiErrorType type) {
        return respond(type, messageService.getMessage(type.messageKey()));
    }

    private ResponseEntity<ApiResult<Void>> respond(ApiErrorType type, String message) {
        String title = messageService.getMessage(type.titleKey());
        return ResponseEntity.status(type.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResult.error(type, title, message));
    }
}
