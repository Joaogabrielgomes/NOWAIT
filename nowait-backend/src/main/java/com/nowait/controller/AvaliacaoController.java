package com.nowait.controller;

import com.nowait.dto.AvaliacaoPendenteDTO;
import com.nowait.dto.AvaliacaoRequestDTO;
import com.nowait.dto.AvaliacaoResponseDTO;
import com.nowait.dto.AvaliacaoResumoDTO;
import com.nowait.model.Usuario;
import com.nowait.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping("/api/avaliacoes")
    public ResponseEntity<AvaliacaoResponseDTO> avaliar(
            @Valid @RequestBody AvaliacaoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(avaliacaoService.avaliar(dto, usuario));
    }

    @GetMapping("/api/avaliacoes/pendentes")
    public ResponseEntity<List<AvaliacaoPendenteDTO>> listarPendentes(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(avaliacaoService.listarPendentes(usuario));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarPorEstabelecimento(
            @PathVariable String estabelecimentoId) {
        return ResponseEntity.ok(avaliacaoService.listarPorEstabelecimento(estabelecimentoId));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes/resumo")
    public ResponseEntity<AvaliacaoResumoDTO> resumoPublico(@PathVariable String estabelecimentoId) {
        return ResponseEntity.ok(avaliacaoService.resumoPublico(estabelecimentoId));
    }
}
