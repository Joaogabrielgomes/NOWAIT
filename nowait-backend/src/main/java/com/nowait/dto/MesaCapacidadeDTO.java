package com.nowait.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesaCapacidadeDTO {

    @NotNull(message = "Capacidade é obrigatória")
    @Min(value = 1, message = "Capacidade deve ser no mínimo 1")
    @Max(value = 50, message = "Capacidade deve ser no máximo 50")
    private Integer capacidade;
}
