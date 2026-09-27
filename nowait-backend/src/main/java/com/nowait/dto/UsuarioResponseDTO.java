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
public class UsuarioResponseDTO {

    private String id;
    private String nome;
    private String email;
    private String role;
    private EnderecoDTO endereco;
    private LocalDateTime createdAt;
}
