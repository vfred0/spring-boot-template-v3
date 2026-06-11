package com.template.service.core.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.client.ClientResponse;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.exceptions.PhoneAlreadyExistsException;
import com.template.data.entities.core.request.Request;
import com.template.data.entities.core.request.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.template.service.core.ClientService;
import com.template.service.core.shared.MessageService;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProcessingService {

    private final RequestStateService requestStateService;
    private final ClientService clientService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    public void processClientCreateRequest(UUID requestId) {
        Request request = requestStateService.getRequired(requestId);
        requestStateService.markProcessing(requestId);

        try {
            if (request.getType() != RequestType.CLIENT_CREATE) {
                throw new IllegalStateException("Unsupported request type: " + request.getType());
            }

            CreateClientRequest createClientRequest = objectMapper.readValue(request.getRequestData(), CreateClientRequest.class);
            ClientResponse clientResponse = clientService.create(createClientRequest);
            requestStateService.markCompleted(requestId, writeJson(ApiResult.ok(clientResponse)));
        } catch (PhoneAlreadyExistsException ex) {
            log.warn("Request {} failed due to duplicate phone", requestId, ex);
            requestStateService.markFailed(requestId, writeJson(
                    ApiResult.error(ApiErrorType.CONFLICT,
                            messageService.getMessage(ex.getMessageCode(), new Object[]{ex.getPhone()}))
            ));
        } catch (Exception ex) {
            log.error("Request {} processing failed", requestId, ex);
            requestStateService.markFailed(requestId, writeJson(
                    ApiResult.error(ApiErrorType.INTERNAL_SERVER_ERROR,
                            messageService.getMessage("api.error.internalServerError"))
            ));
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request processing payload", ex);
        }
    }
}

