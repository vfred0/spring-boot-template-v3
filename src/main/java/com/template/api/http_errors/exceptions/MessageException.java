package com.template.api.http_errors.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageException {

    USER_NOT_FOUND("Usuario no encontrado"),
    DATA_NOT_FOUND("Datos no encontrados"),
    INVALID_ACCESS_TOKEN("Token de acceso inválido"),
    UNAUTHORIZED("Acceso no autorizado"),
    NOT_FOUND("Recurso no encontrado"),
    BAD_REQUEST("Solicitud incorrecta"),
    INTERNAL_SERVER_ERROR("Error interno del servidor"),
    BAD_REQUEST_JSON_PARSE("Error de parseo JSON: Asegúrese de que el formato del JSON es correcto y coincide con la estructura esperada."),
    CONFLICT("Conflicto"),
    FORBIDDEN("Acceso prohibido"),
    UNSUPPORTED_MEDIA_TYPE("Tipo de medio no soportado"),
    SERVICE_UNAVAILABLE("Servicio temporalmente no disponible"),
    INVALID_API_KEY("API KEY inválida"),
    INVALID_ENUM_VALUE("Valor de enumeración inválido"),
    MISSING_API_KEY("Falta la API KEY"),
    METHOD_NOT_ALLOWED("Método no permitido"),
    INVALID_API_VERSION("Versión de API inválida o no soportada")
    ;

    private String message;

    public MessageException withDetail(String detail) {
        if (this.message.contains(":")) {
            this.message = this.message.substring(0, this.message.indexOf(":"));
        }
        this.message = String.format("%s: %s", this.message, detail);
        return this;
    }

}