package com.nowait.service;

import com.nowait.exception.BusinessRuleException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.MesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes do algoritmo de verificação de capacidade de agendamento: o limite de mesas
 * reservadas por horário (50% do total) e o empacotamento de demandas simultâneas nas
 * mesas disponíveis (bin-packing simplificado por ordenação).
 */
@ExtendWith(MockitoExtension.class)
class VerificadorCapacidadeAgendamentoTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private MesaRepository mesaRepository;

    @InjectMocks
    private VerificadorCapacidadeAgendamento verificador;

    private Estabelecimento estabelecimento;
    private final LocalDateTime dataHora = LocalDateTime.of(2026, 12, 1, 19, 0);
    private final List<String> statusConsiderados = List.of("PENDENTE", "CONFIRMADO");

    @BeforeEach
    void setUp() {
        estabelecimento = Estabelecimento.builder().id("est-1").tempoMedioAtendimentoMinutos(60).build();
    }

    @Test
    void validarLimitePorHorario_lancaExcecaoQuandoAtingiuOLimite() {
        when(mesaRepository.countByEstabelecimentoId("est-1")).thenReturn(10L);
        when(agendamentoRepository.countByEstabelecimentoIdAndStatusInAndDataHora("est-1", statusConsiderados, dataHora))
                .thenReturn(5L); // limite = floor(10 * 0.5) = 5

        assertThatThrownBy(() -> verificador.validarLimitePorHorario(estabelecimento, dataHora, statusConsiderados))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validarLimitePorHorario_naoLancaExcecaoQuandoAbaixoDoLimite() {
        when(mesaRepository.countByEstabelecimentoId("est-1")).thenReturn(10L);
        when(agendamentoRepository.countByEstabelecimentoIdAndStatusInAndDataHora("est-1", statusConsiderados, dataHora))
                .thenReturn(2L);

        assertThatCode(() -> verificador.validarLimitePorHorario(estabelecimento, dataHora, statusConsiderados))
                .doesNotThrowAnyException();
    }

    @Test
    void validar_lancaExcecaoQuandoNaoHaMesaComCapacidadeSuficiente() {
        when(agendamentoRepository.findByEstabelecimentoIdAndStatusInAndDataHoraBetween(
                anyString(), any(), any(), any())).thenReturn(List.of());
        when(mesaRepository.findByEstabelecimentoIdOrderByNumeroAsc("est-1")).thenReturn(List.of(
                Mesa.builder().id("m-1").capacidade(2).build(),
                Mesa.builder().id("m-2").capacidade(4).build()
        ));

        assertThatThrownBy(() -> verificador.validar(estabelecimento, dataHora, 6, statusConsiderados))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validar_naoLancaExcecaoQuandoExisteMesaComCapacidadeSuficiente() {
        when(agendamentoRepository.findByEstabelecimentoIdAndStatusInAndDataHoraBetween(
                anyString(), any(), any(), any())).thenReturn(List.of());
        when(mesaRepository.findByEstabelecimentoIdOrderByNumeroAsc("est-1")).thenReturn(List.of(
                Mesa.builder().id("m-1").capacidade(2).build(),
                Mesa.builder().id("m-2").capacidade(6).build()
        ));

        assertThatCode(() -> verificador.validar(estabelecimento, dataHora, 4, statusConsiderados))
                .doesNotThrowAnyException();
    }
}
