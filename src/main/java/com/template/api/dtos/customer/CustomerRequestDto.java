package com.template.api.dtos.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerRequestDto {

    @NotBlank(message = "El odoo ID es obligatorio")
    String odooId;

    @NotBlank(message = "El id es obligatorio")
    String id;

    @NotBlank(message = "El identificador (RUC o Cedula) es obligatorio")
    @Pattern(regexp = "\\d{10,13}", message = "El El identificador (RUC o Cedula) debe contener entre 10 y 13 dígitos")
    String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    String names;


    Set<String> emails;

    String phone;

    @Valid
    PaymentTermDto paymentTerm;

    @NotNull(message = "El límite de crédito es obligatorio")
    @DecimalMin(value = "0.0", message = "El límite de crédito debe ser mayor o igual a 0")
    Double creditLimit;

    @NotNull(message = "El estado de archivado es obligatorio")
    Boolean isArchived;

    @NotNull(message = "Las direcciones son obligatorias")
    @Valid
    Set<AddressDto> addresses;

    Set<String> regionals;

    String channel;

    String segment;

    String commercial;

    String salesTeam;

    String province;

    String city;

    @NotNull(message = "El estado es obligatorio")
    CustomerStatus status;
}
