package com.nowait.controller;

import com.nowait.dto.EstabelecimentoRequestDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.EstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService estabelecimentoService;

    @PostMapping
    public ResponseEntity<EstabelecimentoResponseDTO> criar(
            @Valid @RequestBody EstabelecimentoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(estabelecimentoService.criar(dto, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstabelecimentoResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody EstabelecimentoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.atualizar(id, dto, usuario));
    }

    @GetMapping
    public ResponseEntity<List<EstabelecimentoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estabelecimentoService.listarTodos());
    }

    @GetMapping("/me")
    public ResponseEntity<EstabelecimentoResponseDTO> meuEstabelecimento(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.buscarPorDono(usuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstabelecimentoResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(estabelecimentoService.buscarPorId(id));
    }
}
