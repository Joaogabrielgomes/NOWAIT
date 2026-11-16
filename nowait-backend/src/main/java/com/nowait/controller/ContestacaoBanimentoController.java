package com.nowait.controller;

import com.nowait.dto.ContestacaoBanimentoInfoDTO;
import com.nowait.dto.ContestacaoBanimentoRequestDTO;
import com.nowait.service.ContestacaoBanimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contestacoes-banimento")
@RequiredArgsConstructor
public class ContestacaoBanimentoController {

    private final ContestacaoBanimentoService contestacaoBanimentoService;

    @GetMapping("/{token}")
    public ResponseEntity<ContestacaoBanimentoInfoDTO> buscarPorToken(@PathVariable String token) {
        return ResponseEntity.ok(contestacaoBanimentoService.buscarInfoPorToken(token));
    }

    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody ContestacaoBanimentoRequestDTO dto) {
        contestacaoBanimentoService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
