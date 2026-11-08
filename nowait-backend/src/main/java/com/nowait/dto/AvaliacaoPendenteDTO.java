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
public class AvaliacaoPendenteDTO {

    private String tipo;
    private String referenciaId;
    private String estabelecimentoId;
    private String estabelecimentoNome;
    private LocalDateTime data;
}
