package com.template.api.dtos.customer;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Builder
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentTermDto {
    @NotNull(message = "El ID del término de pago es obligatorio")
    @Digits(integer = 5, fraction = 0, message = "El ID del término de pago debe tener hasta 5 dígitos")
    @Min(value = 1, message = "El ID del término de pago debe ser mayor que cero")
    Integer id;

    @NotNull(message = "El término de pago es obligatorio")
    @Size(min = 3, max = 80, message = "El término de pago debe tener entre 3 y 80 caracteres")
    String name;

    @JsonIgnore
    public boolean containsName() {
        return name != null && !name.trim().isEmpty();
    }
}
