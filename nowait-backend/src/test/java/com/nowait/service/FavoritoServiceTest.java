package com.nowait.service;

import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Favorito;
import com.nowait.model.Usuario;
import com.nowait.repository.FavoritoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do FavoritoService: adicionar (idempotente — não duplica se já existe), remover
 * (erro quando não existe) e as duas listagens usadas pelo frontend (detalhes completos e
 * apenas os ids, para marcar o coração nos cards da listagem pública).
 */
@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock private FavoritoRepository favoritoRepository;
    @Mock private EstabelecimentoService estabelecimentoService;

    @InjectMocks
    private FavoritoService favoritoService;

    private Usuario cliente;
    private Estabelecimento estabelecimento;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().id("usr-1").build();
        estabelecimento = Estabelecimento.builder().id("est-1").nome("Restaurante").build();
    }

    @Test
    void adicionar_naoDuplicaQuandoJaExiste() {
        when(favoritoRepository.existsByClienteIdAndEstabelecimentoId("usr-1", "est-1")).thenReturn(true);

        favoritoService.adicionar("est-1", cliente);

        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void adicionar_persisteNovoFavorito() {
        when(favoritoRepository.existsByClienteIdAndEstabelecimentoId("usr-1", "est-1")).thenReturn(false);
        when(estabelecimentoService.buscarEntidadePorId("est-1")).thenReturn(estabelecimento);

        favoritoService.adicionar("est-1", cliente);

        verify(favoritoRepository).save(any(Favorito.class));
    }

    @Test
    void remover_lancaExcecaoQuandoFavoritoNaoExiste() {
        when(favoritoRepository.findByClienteIdAndEstabelecimentoId("usr-1", "est-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.remover("est-1", cliente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void remover_deletaFavoritoExistente() {
        Favorito favorito = Favorito.builder().id("fav-1").cliente(cliente).estabelecimento(estabelecimento).build();
        when(favoritoRepository.findByClienteIdAndEstabelecimentoId("usr-1", "est-1")).thenReturn(Optional.of(favorito));

        favoritoService.remover("est-1", cliente);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    void listarMeus_retornaEstabelecimentosFavoritados() {
        Favorito favorito = Favorito.builder().id("fav-1").cliente(cliente).estabelecimento(estabelecimento).build();
        when(favoritoRepository.findByClienteIdOrderByCreatedAtDesc("usr-1")).thenReturn(List.of(favorito));
        when(estabelecimentoService.buscarPorId("est-1"))
                .thenReturn(EstabelecimentoResponseDTO.builder().id("est-1").nome("Restaurante").build());

        List<EstabelecimentoResponseDTO> resultado = favoritoService.listarMeus(cliente);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Restaurante");
    }

    @Test
    void listarIdsFavoritos_retornaConjuntoDeIds() {
        Favorito favorito = Favorito.builder().id("fav-1").cliente(cliente).estabelecimento(estabelecimento).build();
        when(favoritoRepository.findByClienteIdOrderByCreatedAtDesc("usr-1")).thenReturn(List.of(favorito));

        Set<String> ids = favoritoService.listarIdsFavoritos(cliente);

        assertThat(ids).containsExactly("est-1");
    }
}
