package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaResponseDTO {

    private String id;
    private String estabelecimentoId;
    private String numero;
    private Integer capacidade;
    private String status;
}
