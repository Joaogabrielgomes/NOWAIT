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
public class AvaliacaoResponseDTO {

    private String id;
    private String estabelecimentoId;
    private String estabelecimentoNome;
    private String clienteId;
    private String clienteNome;
    private Integer nota;
    private String comentario;
    private LocalDateTime createdAt;
}
