package com.nowait.service;

import com.nowait.dto.AgendamentoMotivoDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.Agendamento;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.model.StatusAgendamento;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Testes do fluxo de agendamento: aceite (PENDENTE -> CONFIRMADO), cancelamento (com
 * liberação de mesa) e expiração automática por tolerância (o job agendado que
 * cancela agendamentos cujo prazo de tolerância já passou).
 */
@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private MesaRepository mesaRepository;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private AlocacaoMesaService alocacaoMesaService;
    @Mock private VerificadorCapacidadeAgendamento verificadorCapacidade;
    @Mock private AuditoriaService auditoriaService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Estabelecimento estabelecimento;
    private Usuario cliente;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        dono = Usuario.builder().id("dono-1").nome("Dono").email("dono@exemplo.com").build();
        estabelecimento = Estabelecimento.builder()
                .id("estab-1").usuario(dono).nome("Restaurante Teste").telefone("11999999999")
                .build();
        cliente = Usuario.builder().id("cliente-1").nome("Cliente").email("cliente@exemplo.com").build();

        lenient().when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aceitar_agendamentoPendente_confirmaEValidaCapacidade() {
        Agendamento agendamento = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).dataHora(LocalDateTime.now().plusHours(2))
                .status(StatusAgendamento.PENDENTE)
                .build();
        when(agendamentoRepository.findById("ag-1")).thenReturn(java.util.Optional.of(agendamento));

        var resposta = agendamentoService.aceitar("ag-1", dono);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(resposta.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verify(verificadorCapacidade).validar(eq(estabelecimento), any(), eq(2), anyList());
        verify(verificadorCapacidade).validarLimitePorHorario(eq(estabelecimento), any(), anyList());
    }

    @Test
    void aceitar_agendamentoJaConfirmado_lancaBusinessRuleExceptionENaoChamaVerificador() {
        Agendamento agendamento = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).dataHora(LocalDateTime.now().plusHours(2))
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findById("ag-1")).thenReturn(java.util.Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.aceitar("ag-1", dono))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDENTES");

        verifyNoInteractions(verificadorCapacidade);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void cancelar_peloCliente_liberaMesaENaoExigeMotivo() {
        Mesa mesa = Mesa.builder().id("mesa-1").numero("3").capacidade(4).status(StatusMesa.OCUPADA).build();
        Agendamento agendamento = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente).mesa(mesa)
                .quantidadePessoas(2).dataHora(LocalDateTime.now().plusHours(2))
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findById("ag-1")).thenReturn(java.util.Optional.of(agendamento));

        var resposta = agendamentoService.cancelar("ag-1", new AgendamentoMotivoDTO(), cliente);

        assertThat(mesa.getStatus()).isEqualTo(StatusMesa.LIVRE);
        assertThat(resposta.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(resposta.getMotivoCancelamento()).isNull();
        verify(mesaRepository).save(mesa);
    }

    @Test
    void cancelar_peloDonoSemMotivo_lancaBusinessRuleException() {
        Agendamento agendamento = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).dataHora(LocalDateTime.now().plusHours(2))
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findById("ag-1")).thenReturn(java.util.Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.cancelar("ag-1", new AgendamentoMotivoDTO(), dono))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("motivo");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void cancelar_porUsuarioSemRelacao_lancaUnauthorizedOperationException() {
        Agendamento agendamento = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).dataHora(LocalDateTime.now().plusHours(2))
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findById("ag-1")).thenReturn(java.util.Optional.of(agendamento));
        Usuario terceiro = Usuario.builder().id("outro").nome("Outro").email("outro@exemplo.com").build();

        assertThatThrownBy(() -> agendamentoService.cancelar("ag-1", new AgendamentoMotivoDTO(), terceiro))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void expirarAgendamentosVencidos_prazoDeToleranciaEsgotado_marcaExpiradoELiberaMesa() {
        Mesa mesa = Mesa.builder().id("mesa-1").numero("3").capacidade(4).status(StatusMesa.OCUPADA).build();
        Agendamento vencido = Agendamento.builder()
                .id("ag-1").estabelecimento(estabelecimento).cliente(cliente).mesa(mesa)
                .quantidadePessoas(2)
                .dataHora(LocalDateTime.now().minusHours(2))
                .toleranciaMinutos(15)
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findByStatusInAndDataHoraBefore(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(vencido));

        agendamentoService.expirarAgendamentosVencidos();

        assertThat(vencido.getStatus()).isEqualTo(StatusAgendamento.EXPIRADO);
        assertThat(mesa.getStatus()).isEqualTo(StatusMesa.LIVRE);
        verify(agendamentoRepository).save(vencido);
    }

    @Test
    void expirarAgendamentosVencidos_aindaDentroDaTolerancia_naoAlteraStatus() {
        Agendamento dentroDoPrazo = Agendamento.builder()
                .id("ag-2").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2)
                .dataHora(LocalDateTime.now().minusMinutes(5))
                .toleranciaMinutos(60)
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findByStatusInAndDataHoraBefore(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(dentroDoPrazo));

        agendamentoService.expirarAgendamentosVencidos();

        assertThat(dentroDoPrazo.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verify(agendamentoRepository, never()).save(any());
    }
}
