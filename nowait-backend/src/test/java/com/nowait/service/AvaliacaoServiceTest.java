package com.nowait.service;

import com.nowait.dto.AvaliacaoRequestDTO;
import com.nowait.dto.AvaliacaoResponseDTO;
import com.nowait.dto.AvaliacaoResumoDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.FilaEspera;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.AvaliacaoRepository;
import com.nowait.repository.FilaEsperaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do AvaliacaoService: criação a partir de uma entrada de fila (permissão, liberação
 * e duplicidade), resumo público (média/contagem) e listagem de avaliações pendentes.
 */
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private FilaEsperaRepository filaEsperaRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private Usuario cliente;
    private Estabelecimento estabelecimento;
    private FilaEspera entradaFila;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().id("usr-1").nome("Cliente").build();
        estabelecimento = Estabelecimento.builder().id("est-1").nome("Restaurante").build();
        entradaFila = FilaEspera.builder().id("fila-1").cliente(cliente).estabelecimento(estabelecimento)
                .avaliacaoLiberada(true).build();
    }

    private AvaliacaoRequestDTO dtoDeFila(int nota) {
        AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO();
        dto.setFilaId("fila-1");
        dto.setNota(nota);
        dto.setComentario("Muito bom!");
        return dto;
    }

    @Test
    void avaliar_lancaExcecaoQuandoClienteNaoEDonoDaEntradaDeFila() {
        Usuario outroCliente = Usuario.builder().id("usr-2").build();
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entradaFila));

        assertThatThrownBy(() -> avaliacaoService.avaliar(dtoDeFila(5), outroCliente))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void avaliar_lancaExcecaoQuandoAvaliacaoAindaNaoLiberada() {
        entradaFila.setAvaliacaoLiberada(false);
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entradaFila));

        assertThatThrownBy(() -> avaliacaoService.avaliar(dtoDeFila(5), cliente))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void avaliar_lancaExcecaoQuandoJaExisteAvaliacaoParaEssaFila() {
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entradaFila));
        when(avaliacaoRepository.existsByFilaId("fila-1")).thenReturn(true);

        assertThatThrownBy(() -> avaliacaoService.avaliar(dtoDeFila(5), cliente))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void avaliar_persisteAvaliacaoComNotaEComentario() {
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entradaFila));
        when(avaliacaoRepository.existsByFilaId("fila-1")).thenReturn(false);
        when(avaliacaoRepository.save(any())).thenAnswer(inv -> {
            var avaliacao = inv.getArgument(0, com.nowait.model.Avaliacao.class);
            avaliacao.setId("aval-1");
            return avaliacao;
        });

        AvaliacaoResponseDTO resultado = avaliacaoService.avaliar(dtoDeFila(5), cliente);

        assertThat(resultado.getNota()).isEqualTo(5);
        assertThat(resultado.getComentario()).isEqualTo("Muito bom!");
        verify(auditoriaService).registrar(eq(cliente), any(), eq("Avaliacao"), eq("aval-1"), anyString());
    }

    @Test
    void resumoPublico_retornaMediaETotalDoRepositorio() {
        when(avaliacaoRepository.calcularMediaPorEstabelecimento("est-1")).thenReturn(4.5);
        when(avaliacaoRepository.countByEstabelecimentoId("est-1")).thenReturn(10L);

        AvaliacaoResumoDTO resumo = avaliacaoService.resumoPublico("est-1");

        assertThat(resumo.getMediaNota()).isEqualTo(4.5);
        assertThat(resumo.getTotalAvaliacoes()).isEqualTo(10L);
    }

    @Test
    void resumoPublicoLote_retornaUmResumoPorEstabelecimentoSemDuplicar() {
        when(avaliacaoRepository.calcularMediaPorEstabelecimento(anyString())).thenReturn(4.0);
        when(avaliacaoRepository.countByEstabelecimentoId(anyString())).thenReturn(2L);

        var resultado = avaliacaoService.resumoPublicoLote(List.of("est-1", "est-1", "est-2"));

        assertThat(resultado).hasSize(2);
        verify(avaliacaoRepository, times(1)).calcularMediaPorEstabelecimento("est-1");
        verify(avaliacaoRepository, times(1)).calcularMediaPorEstabelecimento("est-2");
    }

    @Test
    void listarPendentes_incluiEntradaDeFilaAtendidaESemAvaliacaoAinda() {
        when(filaEsperaRepository.findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByHoraAtendimentoDesc(
                eq("usr-1"), any())).thenReturn(List.of(entradaFila));
        when(avaliacaoRepository.existsByFilaId("fila-1")).thenReturn(false);
        when(agendamentoRepository.findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByUpdatedAtDesc(
                eq("usr-1"), any())).thenReturn(List.of());

        var pendentes = avaliacaoService.listarPendentes(cliente);

        assertThat(pendentes).hasSize(1);
        assertThat(pendentes.get(0).getReferenciaId()).isEqualTo("fila-1");
    }
}
