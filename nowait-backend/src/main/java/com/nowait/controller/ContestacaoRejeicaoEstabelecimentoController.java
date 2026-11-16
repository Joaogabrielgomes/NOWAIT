package com.nowait.controller;

import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoInfoDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoRequestDTO;
import com.nowait.service.ContestacaoRejeicaoEstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contestacoes-rejeicao-estabelecimento")
@RequiredArgsConstructor
public class ContestacaoRejeicaoEstabelecimentoController {

    private final ContestacaoRejeicaoEstabelecimentoService contestacaoRejeicaoEstabelecimentoService;

    @GetMapping("/{token}")
    public ResponseEntity<ContestacaoRejeicaoEstabelecimentoInfoDTO> buscarPorToken(@PathVariable String token) {
        return ResponseEntity.ok(contestacaoRejeicaoEstabelecimentoService.buscarInfoPorToken(token));
    }

    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody ContestacaoRejeicaoEstabelecimentoRequestDTO dto) {
        contestacaoRejeicaoEstabelecimentoService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
