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
public class FilaEsperaResponseDTO {

    private String id;
    private String estabelecimentoId;
    private String clienteId;
    private String clienteNome;
    private String mesaId;
    private Integer quantidadePessoas;
    private String status;
    private Integer posicao;
    private Integer tempoEsperaEstimadoMinutos;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaChamado;
    private LocalDateTime horaAtendimento;
}
