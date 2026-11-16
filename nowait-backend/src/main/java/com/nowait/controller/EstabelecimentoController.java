package com.nowait.controller;

import com.nowait.dto.EstabelecimentoRequestDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.EstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EstabelecimentoResponseDTO> atualizarLogo(
            @PathVariable String id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.atualizarLogo(id, arquivo, usuario));
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EstabelecimentoResponseDTO> atualizarBanner(
            @PathVariable String id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.atualizarBanner(id, arquivo, usuario));
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<EstabelecimentoResponseDTO> removerLogo(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.removerLogo(id, usuario));
    }

    @DeleteMapping("/{id}/banner")
    public ResponseEntity<EstabelecimentoResponseDTO> removerBanner(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.removerBanner(id, usuario));
    }

    @PatchMapping("/{id}/aberto")
    public ResponseEntity<EstabelecimentoResponseDTO> alternarAberto(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(estabelecimentoService.alternarAberto(id, usuario));
    }

    @GetMapping
    public ResponseEntity<Page<EstabelecimentoResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanho) {
        return ResponseEntity.ok(estabelecimentoService.listarPublico(nome, cidade, bairro, categoria, pagina, tamanho));
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
