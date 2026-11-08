package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaResumoDTO {

    private String estabelecimentoId;
    private Integer quantidadeAguardando;

    private Integer tempoEsperaEstimadoMinutos;
}
