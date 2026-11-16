package com.nowait.controller;

import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping("/api/estabelecimentos/{estabelecimentoId}/favorito")
    public ResponseEntity<Void> adicionar(
            @PathVariable String estabelecimentoId, @AuthenticationPrincipal Usuario usuario) {
        favoritoService.adicionar(estabelecimentoId, usuario);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/api/estabelecimentos/{estabelecimentoId}/favorito")
    public ResponseEntity<Void> remover(
            @PathVariable String estabelecimentoId, @AuthenticationPrincipal Usuario usuario) {
        favoritoService.remover(estabelecimentoId, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/favoritos")
    public ResponseEntity<List<EstabelecimentoResponseDTO>> listarMeus(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(favoritoService.listarMeus(usuario));
    }

    @GetMapping("/api/favoritos/ids")
    public ResponseEntity<Set<String>> listarIds(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(favoritoService.listarIdsFavoritos(usuario));
    }
}
