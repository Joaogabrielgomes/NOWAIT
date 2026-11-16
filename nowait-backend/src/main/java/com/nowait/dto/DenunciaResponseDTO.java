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
public class DenunciaResponseDTO {

    private String id;
    private String denuncianteId;
    private String denuncianteNome;
    private String tipoAlvo;
    private String alvoId;
    private String alvoDescricao;
    private String motivo;
    private String status;
    private String resolvidoPorId;
    private String resolvidoPorNome;
    private String observacaoAdmin;

    private String usuarioAlvoId;

    private LocalDateTime createdAt;
    private LocalDateTime resolvidoEm;
}
