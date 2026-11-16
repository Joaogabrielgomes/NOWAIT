package com.nowait.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BanimentoDTO {

    @NotBlank(message = "Informe o motivo do banimento")
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;
}
