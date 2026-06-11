package com.template.api.dtos.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class CustomerPatchDto {
    String id;

    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    String names;

    String phone;

    Set<String> emails;

    @Valid
    PaymentTermDto paymentTerm;

    @DecimalMin(value = "0.0", message = "El límite de crédito debe ser mayor o igual a 0")
    Double creditLimit;

    Boolean isArchived;

    @Valid
    Set<AddressDto> addresses;

    Set<String> regionals;

    String channel;

    String segment;

    String commercial;

    String salesTeam;

    CustomerStatus status;

    String province;

    String city;

    String region;
}
