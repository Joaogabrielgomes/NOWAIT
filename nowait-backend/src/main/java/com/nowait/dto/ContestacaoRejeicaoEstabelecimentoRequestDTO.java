package com.nowait.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContestacaoRejeicaoEstabelecimentoRequestDTO {

    @NotBlank(message = "Link de contestação inválido")
    private String token;

    @NotBlank(message = "Escreva sua contestação")
    @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
    private String mensagem;
}
