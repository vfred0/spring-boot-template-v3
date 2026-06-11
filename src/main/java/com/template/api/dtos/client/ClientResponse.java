package com.template.api.dtos.client;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String phone
) {}