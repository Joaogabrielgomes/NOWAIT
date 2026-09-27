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
public class EstabelecimentoResponseDTO {

    private String id;
    private String usuarioId;
    private String nome;
    private EnderecoDTO endereco;
    private Integer tempoMedioAtendimentoMinutos;
    private Boolean aberto;
    private LocalDateTime createdAt;
}
