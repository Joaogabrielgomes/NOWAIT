package com.nowait.service;

import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Favorito;
import com.nowait.model.Usuario;
import com.nowait.repository.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository     favoritoRepository;
    private final EstabelecimentoService estabelecimentoService;

    public void adicionar(String estabelecimentoId, Usuario cliente) {
        if (favoritoRepository.existsByClienteIdAndEstabelecimentoId(cliente.getId(), estabelecimentoId)) {
            return;
        }

        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        Favorito favorito = Favorito.builder()
                .cliente(cliente)
                .estabelecimento(estabelecimento)
                .build();

        favoritoRepository.save(favorito);
        log.info("Cliente {} favoritou estabelecimento {}", cliente.getId(), estabelecimentoId);
    }

    public void remover(String estabelecimentoId, Usuario cliente) {
        Favorito favorito = favoritoRepository
                .findByClienteIdAndEstabelecimentoId(cliente.getId(), estabelecimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorito", "estabelecimentoId", estabelecimentoId));

        favoritoRepository.delete(favorito);
        log.info("Cliente {} removeu favorito do estabelecimento {}", cliente.getId(), estabelecimentoId);
    }

    @Transactional(readOnly = true)
    public List<EstabelecimentoResponseDTO> listarMeus(Usuario cliente) {
        return favoritoRepository.findByClienteIdOrderByCreatedAtDesc(cliente.getId()).stream()
                .map(f -> estabelecimentoService.buscarPorId(f.getEstabelecimento().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> listarIdsFavoritos(Usuario cliente) {
        return favoritoRepository.findByClienteIdOrderByCreatedAtDesc(cliente.getId()).stream()
                .map(f -> f.getEstabelecimento().getId())
                .collect(Collectors.toSet());
    }
}
