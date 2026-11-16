package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestacaoBanimentoResponseDTO {

    private String id;
    private String usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
    private String motivoBanimento;

    private boolean usuarioAindaBanido;

    private String mensagem;
    private String status;
    private LocalDateTime createdAt;
}
