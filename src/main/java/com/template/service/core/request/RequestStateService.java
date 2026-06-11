package com.template.service.core.request;

import com.template.api.http_errors.exceptions.RequestNotFoundException;
import com.template.data.entities.core.request.Request;
import com.template.data.daos.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestStateService {

    private final RequestRepository requestRepository;

    @Transactional(readOnly = true)
    public Request getRequired(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markProcessing(now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID requestId, String responseData) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markCompleted(responseData, now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID requestId, String responseData) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markFailed(responseData, now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}


