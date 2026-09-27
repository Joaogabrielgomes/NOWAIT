package com.nowait.dto;

import com.nowait.model.StatusMesa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MesaStatusDTO {

    @NotBlank(message = "Status é obrigatório")
    @Pattern(
        regexp = StatusMesa.LIVRE + "|" + StatusMesa.OCUPADA + "|" + StatusMesa.RESERVADA,
        message = "Status deve ser LIVRE, OCUPADA ou RESERVADA"
    )
    private String status;
}
