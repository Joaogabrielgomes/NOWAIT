package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumoDTO {

    private Map<String, Long> usuariosPorPerfil;

    private long estabelecimentosAbertos;
    private long estabelecimentosFechados;

    private long pessoasNaFilaAgora;

    private List<LogAuditoriaResponseDTO> atividadeRecente;

    private List<AcaoPorDiaDTO> acoesPorDia;

    private List<CrescimentoDiaDTO> crescimentoPorDia;
}
