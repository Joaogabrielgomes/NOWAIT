package com.nowait.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvaliacaoRequestDTO {

    private String filaId;

    private String agendamentoId;

    @NotNull(message = "Nota é obrigatória")
    @Min(value = 1, message = "Nota deve ser entre 1 e 5")
    @Max(value = 5, message = "Nota deve ser entre 1 e 5")
    private Integer nota;

    @Size(max = 500, message = "Comentário deve ter no máximo 500 caracteres")
    private String comentario;

    @AssertTrue(message = "Informe exatamente uma origem para a avaliação: fila ou agendamento")
    public boolean isOrigemValida() {
        return (filaId != null) != (agendamentoId != null);
    }
}
