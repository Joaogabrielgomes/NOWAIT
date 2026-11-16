package com.nowait.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgendamentoRequestDTO {

    @NotNull(message = "Quantidade de pessoas é obrigatória")
    @Min(value = 1, message = "Quantidade de pessoas deve ser no mínimo 1")
    @Max(value = 50, message = "Quantidade de pessoas deve ser no máximo 50")
    private Integer quantidadePessoas;

    @NotNull(message = "Data e hora são obrigatórias")
    @Future(message = "Data e hora do agendamento devem estar no futuro")
    private LocalDateTime dataHora;

    @Min(value = 5, message = "Tolerância mínima é de 5 minutos")
    @Max(value = 120, message = "Tolerância máxima é de 120 minutos")
    private Integer toleranciaMinutos;
}
