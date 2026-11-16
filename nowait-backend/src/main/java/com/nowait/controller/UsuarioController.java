package com.nowait.controller;

import com.nowait.dto.UsuarioAtualizacaoDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.toResponseDTO(usuario));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody UsuarioAtualizacaoDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(usuario, dto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirPropriaConta(@AuthenticationPrincipal Usuario usuario) {
        usuarioService.excluirPropriaConta(usuario);
        return ResponseEntity.noContent().build();
    }
}
