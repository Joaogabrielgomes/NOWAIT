package com.nowait.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejeicaoEstabelecimentoDTO {

    @NotBlank(message = "Informe o motivo da rejeição")
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;
}
