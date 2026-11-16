package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcaoPorDiaDTO {

    private LocalDate dia;
    private String acao;
    private long quantidade;
}
