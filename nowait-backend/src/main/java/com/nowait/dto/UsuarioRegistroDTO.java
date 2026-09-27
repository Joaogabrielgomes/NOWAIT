package com.nowait.dto;

import com.nowait.model.Perfil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioRegistroDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$",
        message = "Senha deve conter pelo menos 1 maiúscula, 1 minúscula e 1 caractere especial"
    )
    private String senha;

    @NotBlank(message = "Perfil é obrigatório")
    @Pattern(regexp = Perfil.CLIENTE + "|" + Perfil.ESTABELECIMENTO, message = "Perfil deve ser CLIENTE ou ESTABELECIMENTO")
    private String role;

    @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade")
    private boolean aceiteTermos;
}
