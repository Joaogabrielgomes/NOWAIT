package com.nowait.controller;

import com.nowait.dto.MesaRequestDTO;
import com.nowait.dto.MesaResponseDTO;
import com.nowait.dto.MesaStatusDTO;
import com.nowait.model.Usuario;
import com.nowait.service.MesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;

    @PostMapping("/api/estabelecimentos/{estabelecimentoId}/mesas")
    public ResponseEntity<MesaResponseDTO> criar(
            @PathVariable String estabelecimentoId,
            @Valid @RequestBody MesaRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(mesaService.criar(estabelecimentoId, dto, usuario));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/mesas")
    public ResponseEntity<List<MesaResponseDTO>> listar(@PathVariable String estabelecimentoId) {
        return ResponseEntity.ok(mesaService.listarPorEstabelecimento(estabelecimentoId));
    }

    @PutMapping("/api/mesas/{id}")
    public ResponseEntity<MesaResponseDTO> atualizarStatus(
            @PathVariable String id,
            @Valid @RequestBody MesaStatusDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(mesaService.atualizarStatus(id, dto, usuario));
    }

    @PatchMapping("/api/mesas/{id}/liberar")
    public ResponseEntity<MesaResponseDTO> liberar(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(mesaService.liberar(id, usuario));
    }

    @DeleteMapping("/api/mesas/{id}")
    public ResponseEntity<Void> remover(@PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        mesaService.remover(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
