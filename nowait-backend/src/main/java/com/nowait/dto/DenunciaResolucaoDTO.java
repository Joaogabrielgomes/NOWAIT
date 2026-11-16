package com.nowait.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DenunciaResolucaoDTO {

    @NotNull(message = "Informe se a denúncia foi aprovada ou rejeitada")
    private Boolean aprovar;

    private boolean banirUsuarioAlvo;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacaoAdmin;
}
