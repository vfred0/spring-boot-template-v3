package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.auth.RequestStatusResponse;
import com.template.service.core.request.RequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
@Tag(name = "Requests", description = "Asynchronous request management")
@SecurityRequirement(name = "bearerAuth")
public class RequestResource {

    private final RequestService requestService;

    public RequestResource(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_CREATE')")
    @Operation(summary = "Get request status", description = "Returns request status and response payload when the request reaches a terminal state.")
    @ApiResponse(responseCode = "200", description = "Request found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request id",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "404", description = "Request not found",
            content = @Content(mediaType = "application/json"))
    public ApiResult<RequestStatusResponse> get(@PathVariable("id") UUID id) {
        return ApiResult.ok(requestService.getRequestStatus(id));
    }
}

