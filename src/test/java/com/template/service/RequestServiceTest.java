package com.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.api.dtos.auth.RequestAcceptedResponse;
import com.template.api.dtos.auth.RequestStatusResponse;
import com.template.data.entities.core.request.Request;
import com.template.data.entities.core.request.RequestStatus;
import com.template.data.entities.core.request.RequestType;
import com.template.data.daos.RequestRepository;
import com.template.service.core.request.RequestProcessingService;
import com.template.service.core.request.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestProcessingService requestProcessingService;

    private RequestService requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(
                requestRepository,
                requestProcessingService,
                objectMapper
        );
    }

    @Test
    void submitClientCreateRequestStoresSerializedPayloadAndDispatchesProcessing() throws JsonProcessingException {
        CreateClientRequest createClientRequest = new CreateClientRequest("John", "Doe", "+37061234567");
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestAcceptedResponse response = requestService.submitClientCreateRequest(createClientRequest);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(requestCaptor.capture());
        Request savedRequest = requestCaptor.getValue();

        assertThat(response.requestId()).isEqualTo(savedRequest.getId());
        assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(savedRequest.getType()).isEqualTo(RequestType.CLIENT_CREATE);
        assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(savedRequest.getRequestData()).isEqualTo(objectMapper.writeValueAsString(createClientRequest));
        verify(requestProcessingService).processClientCreateRequest(savedRequest.getId());
    }

    @Test
    void getRequestStatusReturnsNestedJsonResponse() {
        UUID requestId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Request request = Request.builder()
                .id(requestId)
                .type(RequestType.CLIENT_CREATE)
                .status(RequestStatus.COMPLETED)
                .createdAt(now)
                .statusChangedAt(now)
                .requestData("{\"firstName\":\"John\"}")
                .responseData("{\"code\":0,\"data\":{\"id\":1,\"phone\":\"+37061234567\"},\"message\":\"OK\"}")
                .build();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        RequestStatusResponse response = requestService.getRequestStatus(requestId);

        assertThat(response.response()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> nestedResponse = (Map<String, Object>) response.response();

        assertThat(nestedResponse)
                .containsEntry("code", 0)
                .containsEntry("message", "OK");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) nestedResponse.get("data");

        assertThat(data)
                .containsEntry("id", 1)
                .containsEntry("phone", "+37061234567");
    }
}




