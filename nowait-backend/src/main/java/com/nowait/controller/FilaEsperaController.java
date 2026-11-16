package com.nowait.controller;

import com.nowait.dto.FilaEsperaRequestDTO;
import com.nowait.dto.FilaEsperaResponseDTO;
import com.nowait.dto.FilaResumoDTO;
import com.nowait.model.Usuario;
import com.nowait.service.FilaEsperaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/fila/resumo")
    public ResponseEntity<FilaResumoDTO> resumoPublico(
            @PathVariable String estabelecimentoId,
            @RequestParam(required = false) Integer pessoas) {
        return ResponseEntity.ok(filaEsperaService.resumoPublico(estabelecimentoId, pessoas));
    }

    @GetMapping("/api/fila/resumo-lote")
    public ResponseEntity<Map<String, FilaResumoDTO>> resumoPublicoLote(@RequestParam List<String> ids) {
        return ResponseEntity.ok(filaEsperaService.resumoPublicoLote(ids));
    }

    @GetMapping("/api/fila/me/ativa")
    public ResponseEntity<FilaEsperaResponseDTO> minhaEntradaAtiva(@AuthenticationPrincipal Usuario usuario) {
        return filaEsperaService.minhaEntradaAtiva(usuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/api/fila/me/historico")
    public ResponseEntity<Page<FilaEsperaResponseDTO>> meuHistorico(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(filaEsperaService.meuHistorico(usuario, pagina, tamanho));
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
