package com.nowait.controller;

import com.nowait.dto.DenunciaCriacaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.model.Usuario;
import com.nowait.service.DenunciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {

    private final DenunciaService denunciaService;

    @PostMapping
    public ResponseEntity<DenunciaResponseDTO> criar(
            @Valid @RequestBody DenunciaCriacaoDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(201).body(denunciaService.criar(usuario, dto));
    }
}
