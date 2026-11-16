package com.nowait.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DenunciaCriacaoDTO {

    @NotBlank(message = "Tipo do alvo é obrigatório")
    @Pattern(regexp = "ESTABELECIMENTO|AVALIACAO|USUARIO", message = "Tipo do alvo inválido")
    private String tipoAlvo;

    @NotBlank(message = "Alvo da denúncia é obrigatório")
    private String alvoId;

    @NotBlank(message = "Informe o motivo da denúncia")
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;
}
