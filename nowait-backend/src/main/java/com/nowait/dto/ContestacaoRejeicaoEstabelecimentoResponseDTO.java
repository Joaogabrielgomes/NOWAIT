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
public class ContestacaoRejeicaoEstabelecimentoResponseDTO {

    private String id;
    private String estabelecimentoId;
    private String estabelecimentoNome;
    private String estabelecimentoEmail;
    private String motivoRejeicao;

    private boolean estabelecimentoAindaRejeitado;

    private String mensagem;
    private String status;
    private LocalDateTime createdAt;
}
