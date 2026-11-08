package com.nowait.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgendamentoMotivoDTO {

    @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres")
    private String motivo;
}
