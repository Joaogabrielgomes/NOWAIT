package com.nowait.controller;

import com.nowait.dto.FilaEsperaRequestDTO;
import com.nowait.dto.FilaEsperaResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.FilaEsperaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FilaEsperaController {

    private final FilaEsperaService filaEsperaService;

    @PostMapping("/api/estabelecimentos/{estabelecimentoId}/fila")
    public ResponseEntity<FilaEsperaResponseDTO> entrar(
            @PathVariable String estabelecimentoId,
            @Valid @RequestBody FilaEsperaRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(filaEsperaService.entrarNaFila(estabelecimentoId, dto, usuario));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/fila")
    public ResponseEntity<List<FilaEsperaResponseDTO>> listar(
            @PathVariable String estabelecimentoId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(filaEsperaService.listarFila(estabelecimentoId, usuario));
    }

    @GetMapping("/api/fila/me/ativa")
    public ResponseEntity<FilaEsperaResponseDTO> minhaEntradaAtiva(@AuthenticationPrincipal Usuario usuario) {
        return filaEsperaService.minhaEntradaAtiva(usuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PatchMapping("/api/fila/{id}/chamar")
    public ResponseEntity<FilaEsperaResponseDTO> chamar(
            @PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(filaEsperaService.chamar(id, usuario));
    }

    @PatchMapping("/api/fila/{id}/atender")
    public ResponseEntity<FilaEsperaResponseDTO> atender(
            @PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(filaEsperaService.atender(id, usuario));
    }

    @PatchMapping("/api/fila/{id}/cancelar")
    public ResponseEntity<FilaEsperaResponseDTO> cancelar(
            @PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(filaEsperaService.cancelar(id, usuario));
    }
}
