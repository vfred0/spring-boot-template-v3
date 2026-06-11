package com.template.api.dtos.auth;

import com.template.data.entities.core.request.RequestStatus;

import java.util.UUID;

public record RequestAcceptedResponse(
        UUID requestId,
        RequestStatus status
) {
}

