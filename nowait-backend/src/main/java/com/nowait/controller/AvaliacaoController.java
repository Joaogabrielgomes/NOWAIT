package com.nowait.controller;

import com.nowait.dto.AvaliacaoEdicaoDTO;
import com.nowait.dto.AvaliacaoPendenteDTO;
import com.nowait.dto.AvaliacaoRequestDTO;
import com.nowait.dto.AvaliacaoResponseDTO;
import com.nowait.dto.AvaliacaoResumoDTO;
import com.nowait.model.Usuario;
import com.nowait.service.AvaliacaoService;
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

    @GetMapping("/api/avaliacoes/minhas")
    public ResponseEntity<Page<AvaliacaoResponseDTO>> listarMinhas(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(avaliacaoService.listarMinhas(usuario, pagina, tamanho));
    }

    @PutMapping("/api/avaliacoes/{id}")
    public ResponseEntity<AvaliacaoResponseDTO> editar(
            @PathVariable String id,
            @Valid @RequestBody AvaliacaoEdicaoDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(avaliacaoService.editar(id, dto, usuario));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes")
    public ResponseEntity<Page<AvaliacaoResponseDTO>> listarPorEstabelecimento(
            @PathVariable String estabelecimentoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(avaliacaoService.listarPorEstabelecimento(estabelecimentoId, pagina, tamanho));
    }

    @GetMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes/resumo")
    public ResponseEntity<AvaliacaoResumoDTO> resumoPublico(@PathVariable String estabelecimentoId) {
        return ResponseEntity.ok(avaliacaoService.resumoPublico(estabelecimentoId));
    }

    @GetMapping("/api/avaliacoes/resumo-lote")
    public ResponseEntity<Map<String, AvaliacaoResumoDTO>> resumoPublicoLote(@RequestParam List<String> ids) {
        return ResponseEntity.ok(avaliacaoService.resumoPublicoLote(ids));
    }
}
