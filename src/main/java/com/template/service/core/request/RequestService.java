package com.template.service.core.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.api.dtos.auth.RequestAcceptedResponse;
import com.template.api.dtos.auth.RequestStatusResponse;
import com.template.api.http_errors.exceptions.RequestNotFoundException;
import com.template.data.entities.core.request.Request;
import com.template.data.entities.core.request.RequestStatus;
import com.template.data.entities.core.request.RequestType;
import com.template.data.daos.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestProcessingService requestProcessingService;
    private final ObjectMapper objectMapper;

    public RequestAcceptedResponse submitClientCreateRequest(CreateClientRequest createClientRequest) {
        OffsetDateTime now = now();
        Request request = Request.builder()
                .id(UUID.randomUUID())
                .type(RequestType.CLIENT_CREATE)
                .status(RequestStatus.PENDING)
                .createdAt(now)
                .statusChangedAt(now)
                .requestData(writeJson(createClientRequest))
                .build();

        Request saved = requestRepository.save(request);
        requestProcessingService.processClientCreateRequest(saved.getId());

        return new RequestAcceptedResponse(saved.getId(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public RequestStatusResponse getRequestStatus(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        return new RequestStatusResponse(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getStatusChangedAt(),
                readJson(request.getResponseData())
        );
    }


    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request payload", ex);
        }
    }

    private Object readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize stored response payload", ex);
        }
    }
}

