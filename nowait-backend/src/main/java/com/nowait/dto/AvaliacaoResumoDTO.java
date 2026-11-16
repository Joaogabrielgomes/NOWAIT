package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoResumoDTO {

    private Double mediaNota;
    private Long totalAvaliacoes;
}
