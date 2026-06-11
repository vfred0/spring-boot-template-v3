package com.template.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.client.ClientResponse;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.api.dtos.auth.RequestAcceptedResponse;
import com.template.service.core.ClientService;
import com.template.service.core.request.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client management")
@SecurityRequirement(name = "bearerAuth")
public class ClientResource {

    private final ClientService clientService;
    private final RequestService requestService;

    public ClientResource(ClientService clientService, RequestService requestService) {
        this.clientService = clientService;
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT_CREATE')")
    @Operation(summary = "Create client", description = "Creates an asynchronous client creation request.")
    @ApiResponse(responseCode = "202", description = "Client creation request accepted",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<ApiResult<RequestAcceptedResponse>> create(@Valid @RequestBody CreateClientRequest req) {
        return ResponseEntity.accepted().body(ApiResult.ok(requestService.submitClientCreateRequest(req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_GET')")
    @Operation(summary = "Get client", description = "Returns client by id.")
    @ApiResponse(responseCode = "200", description = "Client found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(mediaType = "application/json"))
    public ApiResult<ClientResponse> get(@PathVariable("id") Long id) {
        return ApiResult.ok(clientService.get(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT_SEARCH')")
    @Operation(summary = "Search clients", description = "Searches clients by first name or last name. Query minimum length is validated by server, and response size is capped by app.clients.search.max-results.")
    @ApiResponse(responseCode = "200", description = "Search completed",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid query",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json"))
    public ApiResult<List<ClientResponse>> search(@RequestParam("q") String query) {
        return ApiResult.ok(clientService.searchByNameOrSurname(query));
    }
}