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
public class AgendamentoResponseDTO {

    private String id;
    private String estabelecimentoId;
    private String estabelecimentoNome;
    private String clienteId;
    private String clienteNome;
    private String mesaId;
    private Integer quantidadePessoas;
    private LocalDateTime dataHora;
    private Integer toleranciaMinutos;
    private String status;
    private LocalDateTime createdAt;
}
