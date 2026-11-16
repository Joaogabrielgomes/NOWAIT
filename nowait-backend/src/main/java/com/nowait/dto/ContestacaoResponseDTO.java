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
public class ContestacaoResponseDTO {

    private String id;
    private String tipo;
    private String alvoId;
    private String nomeAlvo;
    private String emailAlvo;
    private String motivo;
    private boolean aindaAtivo;
    private String mensagem;
    private String status;
    private LocalDateTime createdAt;
}
