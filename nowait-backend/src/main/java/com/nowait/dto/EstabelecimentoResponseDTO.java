package com.nowait.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstabelecimentoResponseDTO {

    private String id;
    private String usuarioId;
    private String nome;
    private String categoria;
    private String telefone;
    private EnderecoDTO endereco;
    private Integer tempoMedioAtendimentoMinutos;
    private Integer toleranciaPadraoMinutos;
    private LocalTime horarioAbertura;
    private LocalTime horarioFechamento;
    private Integer antecedenciaMaximaDias;
    private Integer antecedenciaReservaMesaMinutos;
    private Integer antecedenciaMinimaMinutos;
    private Boolean aberto;
    private String statusAprovacao;
    private Integer totalMesas;
    private Integer mesasDisponiveis;
    private String logoUrl;
    private String bannerUrl;
    private LocalDateTime createdAt;
}
