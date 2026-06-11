package com.template.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@OpenAPIDefinition(
        info = @Info(
                title = "Servicio web para la sincronización desde Odoo hacia Sistecom", version = "v1",
                contact = @Contact(name = "Víctor Arreaga", email = "varreagae@gnoboa.com")
        ),
        servers = {
                @Server(
                        url = "${openapi.server-url}"
                )
        })
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY");

        Components components = new Components()
                .addSecuritySchemes("X-API-KEY", securityScheme);

        return new OpenAPI()
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList("X-API-KEY"));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, _) -> {
            Schema<?> errorSchema = new Schema<>();
            MediaType mediaType = new MediaType();
            mediaType.setSchema(errorSchema);

            Map<String, Object> validationErrorExample = new LinkedHashMap<>();
            validationErrorExample.put("status", 400);
            validationErrorExample.put("code", "VALIDATION_ERROR");
            validationErrorExample.put("message", "Error de validación en los datos enviados");
            validationErrorExample.put("fieldErrors", List.of(
                    Map.of(
                            "code", "NotNull",
                            "message", "El campo no puede ser nulo",
                            "property", "email",
                            "rejectedValue", "null",
                            "path", "usuario.email"
                    ),
                    Map.of(
                            "code", "Size",
                            "message", "El tamaño debe estar entre 3 y 50",
                            "property", "nombre",
                            "rejectedValue", "ab",
                            "path", "usuario.nombre"
                    )
            ));

            mediaType.setExample(validationErrorExample);
            operation.getResponses().addApiResponse("4XX/5XX", new ApiResponse()
                    .description("Error")
                    .content(new Content().addMediaType("application/json", mediaType)));

            return operation;
        };
    }

}