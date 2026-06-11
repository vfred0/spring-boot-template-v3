package com.template.api.dtos.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto {

    @NotNull(message = "El identificador de la dirección es obligatorio")
    String id;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 3, message = "La dirección debe tener al menos 3 caracteres")
    String address;

    String province;

    String city;
}
