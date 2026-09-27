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
public class LogAuditoriaResponseDTO {

    private String id;
    private String usuarioId;
    private String acao;
    private String entidadeTipo;
    private String entidadeId;
    private String detalhes;
    private String ip;
    private LocalDateTime criadoEm;
}
