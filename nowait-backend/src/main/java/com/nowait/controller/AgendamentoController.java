package com.nowait.controller;

import com.nowait.dto.AgendamentoRequestDTO;
import com.nowait.dto.AgendamentoResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping("/api/estabelecimentos/{estabelecimentoId}/agendamentos")
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @PathVariable String estabelecimentoId,
            @Valid @RequestBody AgendamentoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(agendamentoService.criar(estabelecimentoId, dto, usuario));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/agendamentos")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorEstabelecimento(
            @PathVariable String estabelecimentoId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(agendamentoService.listarPorEstabelecimento(estabelecimentoId, usuario));
    }

    @GetMapping("/api/agendamentos/me")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarMeus(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(agendamentoService.listarMeus(usuario));
    }

    @PatchMapping("/api/agendamentos/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, usuario));
    }

    @PatchMapping("/api/agendamentos/{id}/confirmar-chegada")
    public ResponseEntity<AgendamentoResponseDTO> confirmarChegada(
            @PathVariable String id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(agendamentoService.confirmarChegada(id, usuario));
    }
}
